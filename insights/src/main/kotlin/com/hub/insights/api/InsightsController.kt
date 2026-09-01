package com.hub.insights.api

import com.hub.insights.config.InsightsProperties
import com.hub.insights.domain.derive.BaselineService
import com.hub.insights.domain.derive.CareInsights
import com.hub.insights.domain.derive.MobilityInsights
import com.hub.insights.domain.derive.SleepInsights
import com.hub.insights.domain.find.Finding
import com.hub.insights.application.FindingService
import com.hub.insights.domain.find.SleepBriefing
import com.hub.insights.inbound.HubBathroomDay
import com.hub.insights.inbound.HubCareDay
import com.hub.insights.inbound.HubClient
import com.hub.insights.inbound.HubMobilityDay
import com.hub.insights.inbound.HubSleepDay
import com.hub.insights.domain.recommend.Recommendation
import com.hub.insights.domain.recommend.WellbeingRecommendations
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
    private val findings: FindingService,
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
        val briefing = findings.residentBriefing(residentId, from, to)
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
                cards = briefing?.sleepCards ?: SleepBriefing.cards(derived),
                narrative = briefing?.narrative,
                findings = briefing?.findings.orEmpty().forSleepTab(),
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
        val measured = tab.summaries.filter { it.measured }
        val avg = tab.avgMinutesPerDay ?: CareInsights.avgMinutes(measured.map { it.totalMinutes })
        val share = tab.proactiveShare
        val total = measured.sumOf { it.totalMinutes }
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
        val measured = tab.summaries.filter { it.measured }
        val recs = mutableListOf<Recommendation>()
        if (!baseline.ready) {
            recs += WellbeingRecommendations.forCare(baseline, null, 0).map {
                it.copy(code = "MOBILITY_BASELINE_FORMING")
            }
        }
        val avgWalking = MobilityInsights.avgWalking(measured.map { it.walkingMinutes })
        return ResponseEntity.ok(
            MobilityInsightResponse(
                residentId = residentId,
                from = from.toString(),
                to = to.toString(),
                observedFrom = baseline.observedFrom.toString(),
                baselineReady = baseline.ready,
                summaries = tab.summaries.map { it.toMobilityDto() },
                avgWalkingMinutes = avgWalking,
                estimatedDistanceMeters = avgWalking?.times(properties.walkingMetersPerMinute),
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
        measured = measured,
    )

    private fun HubCareDay.toCareDto() = CareDayDto(
        day = day,
        totalMinutes = totalMinutes,
        proactiveMinutes = proactiveMinutes,
        roundsCount = roundsCount,
        notesCount = notesCount,
        measured = measured,
    )

    private fun HubMobilityDay.toMobilityDto() = MobilityDayDto(
        day = day,
        walkingMinutes = walkingMinutes,
        transferCount = transferCount,
        outOfBedMinutes = outOfBedMinutes,
        inBedMinutes = inBedMinutes,
        outOfSightMinutes = outOfSightMinutes,
        measured = measured,
    )

    private fun HubBathroomDay.toBathroomDto() = BathroomDayDto(
        day = day,
        visitCount = visitCount,
        nightVisitCount = nightVisitCount,
        assistedCount = assistedCount,
        totalMinutes = totalMinutes,
        measured = measured,
    )

    private fun List<Finding>.forSleepTab() = filter { f ->
        f.code.startsWith("SLEEP") ||
            f.code.startsWith("BED_EXIT") ||
            f.code.startsWith("POLICY_BED") ||
            f.code == "BASELINE_FORMING"
    }
}
