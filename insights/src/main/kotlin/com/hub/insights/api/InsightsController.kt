package com.hub.insights.api

import com.hub.insights.config.InsightsProperties
import com.hub.insights.derive.BaselineService
import com.hub.insights.derive.CareInsights
import com.hub.insights.derive.MobilityInsights
import com.hub.insights.derive.SleepInsights
import com.hub.insights.inbound.HubBathroomDay
import com.hub.insights.inbound.HubCareDay
import com.hub.insights.inbound.HubClient
import com.hub.insights.inbound.HubMobilityDay
import com.hub.insights.inbound.HubSleepDay
import com.hub.insights.recommend.Recommendation
import com.hub.insights.recommend.WellbeingRecommendations
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/insights/resident-chart/{residentId}")
class InsightsController(
    private val hub: HubClient,
    private val properties: InsightsProperties,
) {

    @GetMapping("/sleep")
    fun sleep(
        @PathVariable residentId: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): ResponseEntity<SleepInsightResponse> {
        val chart = hub.getChart(residentId) ?: return ResponseEntity.notFound().build()
        val baseline = BaselineService.of(chart.admissionDate, from, to, properties.baselineMinDays)
        val tab = hub.getSleep(residentId, baseline.observedFrom, to)
        val derived = SleepInsights.derive(tab.summaries)
        return ResponseEntity.ok(
            SleepInsightResponse(
                residentId = residentId,
                from = from.toString(),
                to = to.toString(),
                observedFrom = baseline.observedFrom.toString(),
                baselineReady = baseline.ready,
                observedDays = baseline.observedDays,
                summaries = tab.summaries.map { it.toDto() },
                derived = derived.toDto(),
                recommendations = WellbeingRecommendations.forSleep(baseline, derived),
            ),
        )
    }

    @GetMapping("/care")
    fun care(
        @PathVariable residentId: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): ResponseEntity<CareInsightResponse> {
        val chart = hub.getChart(residentId) ?: return ResponseEntity.notFound().build()
        val baseline = BaselineService.of(chart.admissionDate, from, to, properties.baselineMinDays)
        val tab = hub.getCare(residentId, baseline.observedFrom, to)
        val avg = tab.avgMinutesPerDay ?: CareInsights.avgMinutes(tab.summaries.map { it.totalMinutes })
        val share = tab.proactiveShare
            ?: CareInsights.proactiveShare(tab.summaries.sumOf { it.totalMinutes }, tab.summaries.sumOf { it.proactiveMinutes })
        val total = tab.summaries.sumOf { it.totalMinutes }
        return ResponseEntity.ok(
            CareInsightResponse(
                residentId = residentId,
                from = from.toString(),
                to = to.toString(),
                observedFrom = baseline.observedFrom.toString(),
                baselineReady = baseline.ready,
                observedDays = baseline.observedDays,
                summaries = tab.summaries.map { it.toCareDto() },
                avgMinutesPerDay = avg,
                proactiveShare = share,
                recommendations = WellbeingRecommendations.forCare(baseline, avg, total),
            ),
        )
    }

    @GetMapping("/mobility")
    fun mobility(
        @PathVariable residentId: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): ResponseEntity<MobilityInsightResponse> {
        val chart = hub.getChart(residentId) ?: return ResponseEntity.notFound().build()
        val baseline = BaselineService.of(chart.admissionDate, from, to, properties.baselineMinDays)
        val tab = hub.getMobility(residentId, baseline.observedFrom, to)
        val recs = mutableListOf<Recommendation>()
        if (!baseline.ready) {
            recs += WellbeingRecommendations.forCare(baseline, null, 0).map {
                it.copy(code = "MOBILITY_BASELINE_FORMING")
            }
        }
        return ResponseEntity.ok(
            MobilityInsightResponse(
                residentId = residentId,
                from = from.toString(),
                to = to.toString(),
                observedFrom = baseline.observedFrom.toString(),
                baselineReady = baseline.ready,
                summaries = tab.summaries.map { it.toMobilityDto() },
                avgWalkingMinutes = MobilityInsights.avgWalking(tab.summaries.map { it.walkingMinutes }),
                avgDistanceMeters = MobilityInsights.avgDistance(tab.summaries.map { it.distanceMeters }),
                recommendations = recs,
            ),
        )
    }

    @GetMapping("/bathroom")
    fun bathroom(
        @PathVariable residentId: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): ResponseEntity<BathroomInsightResponse> {
        val chart = hub.getChart(residentId) ?: return ResponseEntity.notFound().build()
        val baseline = BaselineService.of(chart.admissionDate, from, to, properties.baselineMinDays)
        val tab = hub.getBathroom(residentId, baseline.observedFrom, to)
        return ResponseEntity.ok(
            BathroomInsightResponse(
                residentId = residentId,
                from = from.toString(),
                to = to.toString(),
                observedFrom = baseline.observedFrom.toString(),
                baselineReady = baseline.ready,
                summaries = tab.summaries.map { it.toBathroomDto() },
            ),
        )
    }

    private fun HubSleepDay.toDto() = SleepDayDto(
        day = day,
        calmMinutes = calmMinutes,
        restlessMinutes = restlessMinutes,
        awakeMinutes = awakeMinutes,
        outOfBedMinutes = outOfBedMinutes,
        bedExitCount = bedExitCount,
        wakeCount = wakeCount,
        startedAt = startedAt?.toString(),
        endedAt = endedAt?.toString(),
    )

    private fun HubCareDay.toCareDto() = CareDayDto(
        day = day,
        totalMinutes = totalMinutes,
        proactiveMinutes = proactiveMinutes,
        roundsCount = roundsCount,
        notesCount = notesCount,
    )

    private fun HubMobilityDay.toMobilityDto() = MobilityDayDto(
        day = day,
        walkingMinutes = walkingMinutes,
        distanceMeters = distanceMeters,
        transferCount = transferCount,
        outOfBedMinutes = outOfBedMinutes,
    )

    private fun HubBathroomDay.toBathroomDto() = BathroomDayDto(
        day = day,
        visitCount = visitCount,
        nightVisitCount = nightVisitCount,
    )
}
