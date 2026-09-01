package com.hub.insights.rollup

import com.hub.insights.config.InsightsProperties
import com.hub.insights.config.ObservationWindow
import com.hub.insights.inbound.BathroomSummaryData
import com.hub.insights.inbound.CareSummaryData
import com.hub.insights.inbound.HubClient
import com.hub.insights.inbound.MobilitySummaryData
import com.hub.insights.inbound.PublishResult
import com.hub.insights.inbound.SleepSummaryData
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

@Service
class RollupService(
    private val hub: HubClient,
    private val properties: InsightsProperties,
) {
    private val window = ObservationWindow.from(properties)

    fun rollupResident(residentId: String, observedOn: LocalDate, publish: Boolean): RollupOutcome {
        val chart = hub.getChart(residentId)
        val admission = chart?.admissionDate
        if (admission != null && observedOn.isBefore(admission)) {
            return RollupOutcome(
                residentId = residentId,
                observedOn = observedOn,
                skipped = true,
                reason = "before_admission",
            )
        }

        val now = Instant.now()
        val sleepRange = window.sleepBounds(observedOn)
        val dayRange = window.calendarDayBounds(observedOn)
        val queryFrom = min(sleepRange.start, dayRange.start).minus(LOOKBACK)
        val queryTo = max(min(sleepRange.endInclusive, now), min(dayRange.endInclusive, now))
        val events = hub.getSceneEvents(residentId, queryFrom, queryTo)
        val points = SceneTimeline.points(events)

        val sleepEnd = min(sleepRange.endInclusive, now)
        val sleepDwells = SceneTimeline.dwells(points, sleepRange.start, sleepEnd)
        val sleep = SleepRollup.compute(
            sleepDwells, observedOn, properties.zoneId, properties.deepSleepAfterMinutes,
        )

        val dayEnd = min(dayRange.endInclusive, now)
        val dayDwells = SceneTimeline.dwells(points, dayRange.start, dayEnd)
        val mobility = MobilityRollup.compute(dayDwells, observedOn)
        val bathroom = BathroomRollup.compute(dayDwells, observedOn, properties.zoneId)
        val care = CareRollup.compute(
            SceneTimeline.staffVisits(events, dayRange.start, dayEnd),
            observedOn,
        )
        val careData = CareSummaryData(
            totalMinutes = care.totalMinutes,
            proactiveMinutes = care.proactiveMinutes,
            roundsCount = care.roundsCount,
            notesCount = care.notesCount,
        )

        val published = mutableMapOf<String, String>()
        if (publish) {
            published["sleep"] = hub.ingestSleep(residentId, observedOn, sleep.toData()).name
            published["mobility"] = hub.ingestMobility(residentId, observedOn, mobility.toData()).name
            published["bathroom"] = hub.ingestBathroom(residentId, observedOn, bathroom.toData()).name
            published["care"] = hub.ingestCare(residentId, observedOn, careData).name
        } else {
            published["sleep"] = PublishResult.Skipped.name
            published["mobility"] = PublishResult.Skipped.name
            published["bathroom"] = PublishResult.Skipped.name
            published["care"] = PublishResult.Skipped.name
        }

        log.info(
            "rollup {} {}: sleep calm={} restless={} awake={} care={}m publish={}",
            residentId, observedOn, sleep.calmMinutes, sleep.restlessMinutes, sleep.awakeMinutes,
            care.totalMinutes, publish,
        )

        return RollupOutcome(
            residentId = residentId,
            observedOn = observedOn,
            skipped = false,
            sleep = sleep.toData(),
            mobility = mobility.toData(),
            bathroom = bathroom.toData(),
            care = careData,
            published = published,
        )
    }

    fun rollupAll(observedOn: LocalDate, publish: Boolean): List<RollupOutcome> {
        val residents = hub.listResidents().filter { it.status.isNullOrBlank() || it.status.equals("ACTIVE", true) }
        return residents.map { rollupResident(it.id, observedOn, publish) }
    }

    private fun min(a: Instant, b: Instant) = if (a.isBefore(b)) a else b
    private fun max(a: Instant, b: Instant) = if (a.isAfter(b)) a else b

    companion object {
        private val log = LoggerFactory.getLogger(RollupService::class.java)
        private val LOOKBACK: Duration = Duration.ofHours(12)
    }
}

data class RollupOutcome(
    val residentId: String,
    val observedOn: LocalDate,
    val skipped: Boolean,
    val reason: String? = null,
    val sleep: SleepSummaryData? = null,
    val mobility: MobilitySummaryData? = null,
    val bathroom: BathroomSummaryData? = null,
    val care: CareSummaryData? = null,
    val published: Map<String, String> = emptyMap(),
)
