package com.hub.insights.rollup

import com.hub.insights.config.InsightsProperties
import com.hub.insights.config.ObservationWindow
import com.hub.insights.inbound.HubClient
import com.hub.insights.inbound.PublishResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate

@Service
class RollupService(
    private val hub: HubClient,
    private val properties: InsightsProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)
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

        val events = hub.getSceneEvents(residentId)
        val points = SceneTimeline.points(events)
        val now = Instant.now()

        val sleepRange = window.sleepBounds(observedOn)
        val sleepEnd = min(sleepRange.endInclusive, now)
        val sleepDwells = SceneTimeline.dwells(points, sleepRange.start, sleepEnd)
        val sleep = SleepRollup.compute(
            sleepDwells, observedOn, properties.zoneId, properties.deepSleepAfterMinutes,
        )

        val dayRange = window.calendarDayBounds(observedOn)
        val dayEnd = min(dayRange.endInclusive, now)
        val dayDwells = SceneTimeline.dwells(points, dayRange.start, dayEnd)
        val mobility = MobilityRollup.compute(dayDwells, observedOn, properties.walkingMetersPerMinute)
        val bathroom = BathroomRollup.compute(dayDwells, observedOn, properties.zoneId)
        val care = CareRollup.compute(
            SceneTimeline.staffVisits(events, dayRange.start, dayEnd),
            observedOn,
        )

        val published = mutableMapOf<String, String>()
        if (publish) {
            published["sleep"] = hub.ingestSleep(residentId, observedOn, sleep.toPayload()).name
            published["mobility"] = hub.ingestMobility(residentId, observedOn, mobility.toPayload()).name
            published["bathroom"] = hub.ingestBathroom(residentId, observedOn, bathroom.toPayload()).name
            published["care"] = hub.ingestCare(
                residentId, observedOn,
                care.totalMinutes, care.proactiveMinutes, care.roundsCount, care.notesCount,
            ).name
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
            sleep = sleep.toPayload(),
            mobility = mobility.toPayload(),
            bathroom = bathroom.toPayload(),
            care = care.toPayload(),
            published = published,
        )
    }

    fun rollupAll(observedOn: LocalDate, publish: Boolean): List<RollupOutcome> {
        val residents = hub.listResidents().filter { it.status.isNullOrBlank() || it.status.equals("ACTIVE", true) }
        return residents.map { rollupResident(it.id, observedOn, publish) }
    }

    private fun min(a: Instant, b: Instant) = if (a.isBefore(b)) a else b
}

data class RollupOutcome(
    val residentId: String,
    val observedOn: LocalDate,
    val skipped: Boolean,
    val reason: String? = null,
    val sleep: Map<String, Any?>? = null,
    val mobility: Map<String, Any?>? = null,
    val bathroom: Map<String, Any?>? = null,
    val care: Map<String, Any?>? = null,
    val published: Map<String, String> = emptyMap(),
)
