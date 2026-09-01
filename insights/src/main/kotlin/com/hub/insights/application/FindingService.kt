package com.hub.insights.application

import com.hub.care.domain.repository.CareSummaryRepository
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
import com.hub.insights.inbound.HubOverrideEntry
import com.hub.insights.inbound.HubSceneEvent
import com.hub.insights.inbound.HubSleepDay
import com.hub.history.domain.repository.HistoryEpisodeDetectionRepository
import com.hub.observation.domain.repository.SceneEventRepository
import com.hub.observation.domain.repository.SummaryRepository
import com.hub.policy.domain.model.PolicyOverride.DwellOverride
import com.hub.policy.domain.repository.AlarmProfileOverrideRepository
import com.hub.policy.domain.repository.AlarmProfileRepository
import com.hub.population.domain.repository.ResidentRepository
import com.hub.shared.domain.ResidentId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class FindingService(
    private val residentRepository: ResidentRepository,
    private val summaryRepository: SummaryRepository,
    private val sceneEventRepository: SceneEventRepository,
    private val careSummaryRepository: CareSummaryRepository,
    private val historyEpisodeRepository: HistoryEpisodeDetectionRepository,
    private val alarmProfileRepository: AlarmProfileRepository,
    private val alarmOverrideRepository: AlarmProfileOverrideRepository,
    private val findingPolicyService: FindingPolicyService,
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
            from = from.toString(), to = to.toString(),
            residentCount = briefing.residentCount,
            baselineForming = briefing.baselineForming,
            toReview = briefing.toReview, positive = briefing.positive,
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
        from = from.toString(), to = to.toString(),
        residentCount = packs.size,
        baselineForming = packs.count { !it.baseline.ready },
        toReview = packs.flatMap { it.toReviewSummaries() },
        positive = packs.flatMap { it.positiveSummaries() },
    )

    private fun loadFacility(from: LocalDate, to: LocalDate): List<ResidentPack> =
        residentRepository.findAll().filter { it.isActive }.mapNotNull { resident ->
            try { load(resident.id.value, from, to) }
            catch (ex: Exception) { log.warn("skip resident={}: {}", resident.id.value, ex.message); null }
        }

    private fun load(residentId: String, from: LocalDate, to: LocalDate): ResidentPack? {
        val rid = ResidentId(residentId)
        val resident = residentRepository.findById(rid) ?: return null
        val baseline = BaselineService.of(resident.admissionDate, from, to, properties.baselineMinDays)

        val sleepSummaries = summaryRepository.findSleepByResidentAndRange(rid, baseline.observedFrom, to)
        val bathroomSummaries = summaryRepository.findBathroomByResidentAndRange(rid, baseline.observedFrom, to)
        val careSummaries = careSummaryRepository.findByResidentAndRange(rid, baseline.observedFrom, to)
        val episodes = historyEpisodeRepository.findByResidentId(rid)
        val profile = alarmProfileRepository.findCurrentByResidentId(rid)
        val overrides = profile?.let { alarmOverrideRepository.findByProfileVersionId(it.id.value) } ?: emptyList()

        val derived = SleepInsights.derive(sleepSummaries.map { it.toHubSleepDay() })

        val clusterFrom = to.minusDays(6)
        val sceneFrom = clusterFrom.atStartOfDay(properties.zoneId).toInstant().minus(LOOKBACK)
        val sceneTo = window.sleepBounds(to).endInclusive
        val sceneEvents = sceneEventRepository.findByResidentId(rid, sceneFrom, sceneTo)
        val points = SceneTimeline.points(sceneEvents.map { it.toHubSceneEvent() })
        val exits = BedExits.fromPoints(points).filter { BedExits.inLocalDays(it, clusterFrom, to, properties.zoneId) }
        val visits = SceneTimeline.staffVisits(sceneEvents.map { it.toHubSceneEvent() }, sceneFrom, sceneTo)
        val staffCount = BedExits.staffAfter(exits, visits).takeIf { it > 0 }
            ?: episodes.count { ep ->
                !ep.selfRecovery &&
                    BedExits.inLocalDays(ep.occurredAt, clusterFrom, to, properties.zoneId) &&
                    exits.any { exit -> Duration.between(exit, ep.occurredAt).abs() <= EPISODE_NEAR }
            }
        val related = episodes.filter { ep ->
            exits.any { exit -> Duration.between(exit, ep.occurredAt).abs() <= EPISODE_NEAR }
        }.map { it.id.value }
        val windowDays = ChronoUnit.DAYS.between(from, to).toInt() + 1
        val riskLevel = profile?.riskLevel?.name?.lowercase()
        val overridesMap = overrides.mapNotNull { o ->
            when (o) {
                is com.hub.policy.domain.model.PolicyOverride.DwellOverride ->
                    o.stateKind to HubOverrideEntry(o.warningAfterMinutes, o.alertAfterMinutes)
                else -> null
            }
        }.toMap()

        val ctx = InsightContext(
            residentId = residentId, residentName = resident.fullName,
            from = from, to = to, baseline = baseline, derived = derived,
            sleepDays = sleepSummaries.map { it.toSleepDayData() },
            bathroomDays = bathroomSummaries.map { it.toBathroomDayData() },
            careAvgMinutes = careSummaries.filter { it.isMeasured }.takeIf { it.isNotEmpty() }
                ?.map { it.totalMinutes }?.average(),
            careTotalMinutes = careSummaries.filter { it.isMeasured }.sumOf { it.totalMinutes },
            exitsLast7d = exits, staffAfterExitCount = staffCount,
            riskLevel = riskLevel,
            bedEdgeWarningMinutes = PolicyCopy.bedEdgeWarningMinutes(riskLevel, overridesMap),
            relatedEpisodeIds = related,
            policyToday = PolicyCopy.spokenLines(riskLevel, overridesMap),
            episodes = episodes.map { EpisodeData(it.id.value, it.kind.name, it.severity.name, it.occurredAt, null) },
            zone = properties.zoneId, windowDays = windowDays,
            sleepPolicy = findingPolicyService.getForResident(residentId).sleep,
            carePolicy = findingPolicyService.getForResident(residentId).care,
            bathroomPolicy = findingPolicyService.getForResident(residentId).bathroom,
        )

        val engineResult = engine.evaluate(ctx)

        return ResidentPack(
            residentId = residentId, residentName = resident.fullName,
            from = from, to = to, observedFrom = baseline.observedFrom,
            baseline = baseline, derived = derived,
            sleepDays = sleepSummaries.map { it.toHubSleepDay() },
            findings = engineResult.findings,
            policyToday = PolicyCopy.spokenLines(riskLevel, overridesMap),
            episodes = episodes.filter { !it.occurredAt.atZone(properties.zoneId).toLocalDate().isBefore(from) },
        )
    }

    // ─── Mappers dominio → pipeline ───

    private fun com.hub.observation.domain.model.SleepSummary.toHubSleepDay() = HubSleepDay(
        day = observedOn.toString(), calmMinutes = calmMinutes, restlessMinutes = restlessMinutes,
        awakeMinutes = awakeMinutes, outOfBedMinutes = outOfBedMinutes,
        bedExitCount = bedExitCount, wakeCount = wakeCount, measured = true,
        startedAt = null, endedAt = null,
    )

    private fun com.hub.observation.domain.model.SleepSummary.toSleepDayData() = SleepDayData(
        day = observedOn.toString(), calmMinutes = calmMinutes, restlessMinutes = restlessMinutes,
        awakeMinutes = awakeMinutes, outOfBedMinutes = outOfBedMinutes,
        bedExitCount = bedExitCount, wakeCount = wakeCount, measured = true,
    )

    private fun com.hub.observation.domain.model.BathroomSummary.toBathroomDayData() = BathroomDayData(
        day = observedOn.toString(), visitCount = visitCount, nightVisitCount = nightVisitCount,
        assistedCount = assistedCount, totalMinutes = totalMinutes, measured = true,
    )

    private fun com.hub.observation.domain.model.SceneEvent.toHubSceneEvent() = HubSceneEvent(
        type = eventType?.name, from = fromState?.name, to = toState?.name,
        at = timestamp, residentId = residentId?.value,
    )

    private val com.hub.care.domain.model.CareSummary.isMeasured: Boolean get() = true

    private data class ResidentPack(
        val residentId: String, val residentName: String?,
        val from: LocalDate, val to: LocalDate, val observedFrom: LocalDate,
        val baseline: Baseline,
        val derived: com.hub.insights.domain.derive.SleepDerived,
        val sleepDays: List<HubSleepDay>,
        val findings: List<Finding>,
        val policyToday: List<String>,
        val episodes: List<com.hub.history.domain.model.HistoryEpisode>,
    ) {
        fun toBriefing() = ResidentBriefing(
            residentId = residentId, residentName = residentName,
            from = from.toString(), to = to.toString(), observedFrom = observedFrom.toString(),
            baselineReady = baseline.ready, observedDays = baseline.observedDays,
            generatedAt = Instant.now(), policyToday = policyToday,
            sleepCards = SleepBriefing.cards(derived),
            narrative = SleepBriefing.narrative(derived, sleepDays),
            findings = findings,
        )

        fun toReport() = ResidentReport(
            residentId = residentId, residentName = residentName,
            from = from.toString(), to = to.toString(), observedFrom = observedFrom.toString(),
            baselineReady = baseline.ready, observedDays = baseline.observedDays,
            generatedAt = Instant.now(), policyToday = policyToday,
            sleepCards = SleepBriefing.cards(derived),
            narrative = SleepBriefing.narrative(derived, sleepDays),
            findings = findings,
            episodes = episodes.map { EpisodeRef(it.id.value, it.kind.name, it.severity.name, it.occurredAt, null) },
        )

        fun toReviewSummaries() = findings.filter { it.kind == FindingKind.POLICY || it.polarity == Polarity.CONCERN }.map { it.toSummary() }
        fun positiveSummaries() = findings.filter { it.polarity == Polarity.POSITIVE }.map { it.toSummary() }
        private fun Finding.toSummary() = ResidentFindingSummary(residentId, residentName, code, kind, polarity, severity, headline, awaitingDecision)
    }

    companion object {
        private val log = LoggerFactory.getLogger(FindingService::class.java)
        private val LOOKBACK = Duration.ofHours(12)
        private val EPISODE_NEAR = Duration.ofMinutes(30)
    }
}
