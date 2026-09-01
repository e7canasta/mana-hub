package com.hub.insights.application

import com.hub.insights.config.InsightsProperties
import com.hub.insights.config.ObservationWindow
import com.hub.insights.domain.derive.Baseline
import com.hub.insights.domain.derive.BaselineService
import com.hub.insights.domain.derive.SleepInsights
import com.hub.insights.domain.find.BedExits
import com.hub.insights.domain.find.EpisodeRef
import com.hub.insights.domain.find.FacilityBriefing
import com.hub.insights.domain.find.FacilityReport
import com.hub.insights.domain.find.Finding
import com.hub.insights.domain.find.FindingKind
import com.hub.insights.domain.find.Polarity
import com.hub.insights.domain.find.PolicyCopy
import com.hub.insights.domain.find.ResidentBriefing
import com.hub.insights.domain.find.ResidentFindingSummary
import com.hub.insights.domain.find.ResidentReport
import com.hub.insights.domain.find.SleepBriefing
import com.hub.insights.domain.rollup.SceneTimeline
import com.hub.insights.engine.InsightContext
import com.hub.insights.engine.InsightEngine
import com.hub.insights.engine.BathroomDayData
import com.hub.insights.engine.EpisodeData
import com.hub.insights.engine.SleepDayData
import com.hub.insights.inbound.HubClient
import com.hub.insights.inbound.HubEpisode
import com.hub.insights.inbound.HubResident
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class FindingService(
    private val hub: HubClient,
    private val properties: InsightsProperties,
    private val engine: InsightEngine = InsightEngine(),
) {
    private val window = ObservationWindow.from(properties)

    fun residentBriefing(residentId: String, from: LocalDate, to: LocalDate): ResidentBriefing? {
        val pack = load(residentId, from, to) ?: return null
        return pack.toBriefing()
    }

    fun residentReport(residentId: String, from: LocalDate, to: LocalDate): ResidentReport? {
        val pack = load(residentId, from, to) ?: return null
        return pack.toReport()
    }

    fun facilityBriefing(from: LocalDate, to: LocalDate): FacilityBriefing =
        toFacilityBriefing(loadFacility(from, to), from, to)

    fun facilityReport(from: LocalDate, to: LocalDate): FacilityReport {
        val packs = loadFacility(from, to)
        val briefing = toFacilityBriefing(packs, from, to)
        return FacilityReport(
            generatedAt = briefing.generatedAt,
            from = from.toString(),
            to = to.toString(),
            residentCount = briefing.residentCount,
            baselineForming = briefing.baselineForming,
            toReview = briefing.toReview,
            positive = briefing.positive,
            residents = packs.map { it.toReport() },
        )
    }

    fun resolveWindow(from: LocalDate?, to: LocalDate?, days: Int): Pair<LocalDate, LocalDate> {
        val end = to ?: LocalDate.now(properties.zoneId)
        val start = from ?: end.minusDays(days.toLong().coerceAtLeast(1) - 1)
        return start to end
    }

    private fun toFacilityBriefing(packs: List<ResidentPack>, from: LocalDate, to: LocalDate) = FacilityBriefing(
        generatedAt = Instant.now(),
        from = from.toString(),
        to = to.toString(),
        residentCount = packs.size,
        baselineForming = packs.count { !it.baseline.ready },
        toReview = packs.flatMap { it.toReviewSummaries() },
        positive = packs.flatMap { it.positiveSummaries() },
    )

    private fun loadFacility(from: LocalDate, to: LocalDate): List<ResidentPack> =
        hub.listResidents().filter { it.active() }.mapNotNull { resident ->
            try {
                load(resident.id, from, to)
            } catch (ex: Exception) {
                log.warn("findings skip resident={}: {}", resident.id, ex.message)
                null
            }
        }

    private fun load(residentId: String, from: LocalDate, to: LocalDate): ResidentPack? {
        val chart = hub.getChart(residentId) ?: return null
        val baseline = BaselineService.of(chart.admissionDate, from, to, properties.baselineMinDays)
        val sleepTab = hub.getSleep(residentId, baseline.observedFrom, to)
        val bathroomTab = hub.getBathroom(residentId, baseline.observedFrom, to)
        val careTab = hub.getCare(residentId, baseline.observedFrom, to)
        val derived = SleepInsights.derive(sleepTab.summaries)
        val presets = hub.getAlarmPresets(residentId)
        val episodes = hub.getEpisodes(residentId).episodes
        val clusterFrom = to.minusDays(6)
        val sceneFrom = clusterFrom.atStartOfDay(properties.zoneId).toInstant().minus(LOOKBACK)
        val sceneTo = window.sleepBounds(to).endInclusive
        val events = hub.getSceneEvents(residentId, sceneFrom, sceneTo)
        val points = SceneTimeline.points(events)
        val exits = BedExits.fromPoints(points).filter {
            BedExits.inLocalDays(it, clusterFrom, to, properties.zoneId)
        }
        val visits = SceneTimeline.staffVisits(events, sceneFrom, sceneTo)
        val staffCount = BedExits.staffAfter(exits, visits).takeIf { it > 0 }
            ?: episodes.count { ep ->
                ep.selfRecovery == false &&
                    BedExits.inLocalDays(ep.occurredAt, clusterFrom, to, properties.zoneId) &&
                    exits.any { exit -> Duration.between(exit, ep.occurredAt).abs() <= EPISODE_NEAR }
            }
        val related = episodes.filter { ep ->
            exits.any { exit -> Duration.between(exit, ep.occurredAt).abs() <= EPISODE_NEAR }
        }.map { it.id }
        val windowDays = ChronoUnit.DAYS.between(from, to).toInt() + 1

        // ─── Construir contexto del pipeline ───
        val ctx = InsightContext(
            residentId = residentId,
            residentName = chart.fullName,
            from = from,
            to = to,
            baseline = baseline,
            derived = derived,
            sleepDays = sleepTab.summaries.map { s ->
                SleepDayData(
                    day = s.day,
                    calmMinutes = s.calmMinutes,
                    restlessMinutes = s.restlessMinutes,
                    awakeMinutes = s.awakeMinutes,
                    outOfBedMinutes = s.outOfBedMinutes,
                    bedExitCount = s.bedExitCount,
                    wakeCount = s.wakeCount,
                    measured = s.measured,
                )
            },
            bathroomDays = bathroomTab.summaries.map { b ->
                BathroomDayData(
                    day = b.day,
                    visitCount = b.visitCount,
                    nightVisitCount = b.nightVisitCount,
                    assistedCount = b.assistedCount,
                    totalMinutes = b.totalMinutes,
                    measured = b.measured,
                )
            },
            careAvgMinutes = careTab.avgMinutesPerDay,
            careTotalMinutes = careTab.summaries.filter { it.measured }.sumOf { it.totalMinutes },
            exitsLast7d = exits,
            staffAfterExitCount = staffCount,
            riskLevel = presets.riskLevel,
            bedEdgeWarningMinutes = PolicyCopy.bedEdgeWarningMinutes(presets.riskLevel, presets.overrides),
            relatedEpisodeIds = related,
            policyToday = PolicyCopy.spokenLines(presets.riskLevel, presets.overrides),
            episodes = episodes.map { ep ->
                EpisodeData(
                    id = ep.id,
                    kind = ep.kind,
                    severity = ep.severity,
                    occurredAt = ep.occurredAt,
                    selfRecovery = ep.selfRecovery,
                )
            },
            zone = properties.zoneId,
            windowDays = windowDays,
        )

        // ─── Ejecutar motor de insights ───
        val engineResult = engine.evaluate(ctx)

        return ResidentPack(
            residentId = residentId,
            residentName = chart.fullName,
            from = from,
            to = to,
            observedFrom = baseline.observedFrom,
            baseline = baseline,
            derived = derived,
            sleepDays = sleepTab.summaries,
            findings = engineResult.findings,
            policyToday = PolicyCopy.spokenLines(presets.riskLevel, presets.overrides),
            episodes = episodes.filter { ep ->
                !ep.occurredAt.atZone(properties.zoneId).toLocalDate().isBefore(from)
            },
        )
    }

    private data class ResidentPack(
        val residentId: String,
        val residentName: String?,
        val from: LocalDate,
        val to: LocalDate,
        val observedFrom: LocalDate,
        val baseline: Baseline,
        val derived: com.hub.insights.domain.derive.SleepDerived,
        val sleepDays: List<com.hub.insights.inbound.HubSleepDay>,
        val findings: List<Finding>,
        val policyToday: List<String>,
        val episodes: List<HubEpisode>,
    ) {
        fun toBriefing() = ResidentBriefing(
            residentId = residentId,
            residentName = residentName,
            from = from.toString(),
            to = to.toString(),
            observedFrom = observedFrom.toString(),
            baselineReady = baseline.ready,
            observedDays = baseline.observedDays,
            generatedAt = Instant.now(),
            policyToday = policyToday,
            sleepCards = SleepBriefing.cards(derived),
            narrative = SleepBriefing.narrative(derived, sleepDays),
            findings = findings,
        )

        fun toReport() = ResidentReport(
            residentId = residentId,
            residentName = residentName,
            from = from.toString(),
            to = to.toString(),
            observedFrom = observedFrom.toString(),
            baselineReady = baseline.ready,
            observedDays = baseline.observedDays,
            generatedAt = Instant.now(),
            policyToday = policyToday,
            sleepCards = SleepBriefing.cards(derived),
            narrative = SleepBriefing.narrative(derived, sleepDays),
            findings = findings,
            episodes = episodes.map {
                EpisodeRef(
                    id = it.id,
                    kind = it.kind ?: "",
                    severity = it.severity,
                    occurredAt = it.occurredAt,
                    selfRecovery = it.selfRecovery,
                )
            },
        )

        fun toReviewSummaries(): List<ResidentFindingSummary> =
            findings.filter { it.kind == FindingKind.POLICY || it.polarity == Polarity.CONCERN }
                .map { it.summary() }

        fun positiveSummaries(): List<ResidentFindingSummary> =
            findings.filter { it.polarity == Polarity.POSITIVE }.map { it.summary() }

        private fun Finding.summary() = ResidentFindingSummary(
            residentId = residentId,
            residentName = residentName,
            code = code,
            kind = kind,
            polarity = polarity,
            severity = severity,
            headline = headline,
            awaitingDecision = awaitingDecision,
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(FindingService::class.java)
        private val LOOKBACK: Duration = Duration.ofHours(12)
        private val EPISODE_NEAR: Duration = Duration.ofMinutes(30)
    }
}

private fun HubResident.active(): Boolean =
    isDischarged != true && status?.contains("discharg", ignoreCase = true) != true
