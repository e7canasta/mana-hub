package com.hub.insights.find

import com.hub.insights.config.InsightsProperties
import com.hub.insights.config.ObservationWindow
import com.hub.insights.derive.BaselineService
import com.hub.insights.derive.SleepInsights
import com.hub.insights.inbound.HubClient
import com.hub.insights.inbound.HubEpisode
import com.hub.insights.inbound.HubResident
import com.hub.insights.rollup.SceneTimeline
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

    private fun toFacilityBriefing(packs: List<ResidentPack>, from: LocalDate, to: LocalDate) = FacilityBriefing(
        generatedAt = Instant.now(),
        from = from.toString(),
        to = to.toString(),
        residentCount = packs.size,
        baselineForming = packs.count { !it.baseline.ready },
        toReview = packs.flatMap { it.toReviewSummaries() },
        positive = packs.flatMap { it.positiveSummaries() },
    )

    fun resolveWindow(from: LocalDate?, to: LocalDate?, days: Int): Pair<LocalDate, LocalDate> {
        val end = to ?: LocalDate.now(properties.zoneId)
        val start = from ?: end.minusDays(days.toLong().coerceAtLeast(1) - 1)
        return start to end
    }

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
        val ctx = FindingContext(
            residentId = residentId,
            residentName = chart.fullName,
            baseline = baseline,
            sleep = derived,
            sleepDays = sleepTab.summaries,
            bathroomDays = bathroomTab.summaries,
            careAvgMinutes = careTab.avgMinutesPerDay,
            exitsLast7d = exits,
            staffAfterExitCount = staffCount,
            riskLevel = presets.riskLevel,
            bedEdgeWarningMinutes = PolicyCopy.bedEdgeWarningMinutes(presets.riskLevel, presets.overrides),
            zone = properties.zoneId,
            windowDays = windowDays,
            relatedEpisodeIds = related,
        )
        return ResidentPack(
            residentId = residentId,
            residentName = chart.fullName,
            from = from,
            to = to,
            observedFrom = baseline.observedFrom,
            baseline = baseline,
            derived = derived,
            sleepDays = sleepTab.summaries,
            findings = FindingCatalog.evaluate(ctx),
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
        val baseline: com.hub.insights.derive.Baseline,
        val derived: com.hub.insights.derive.SleepDerived,
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
