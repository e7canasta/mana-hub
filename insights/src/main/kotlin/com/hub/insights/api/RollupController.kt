package com.hub.insights.api

import com.hub.insights.domain.recommend.WellbeingRecommendations
import com.hub.insights.rollup.RollupService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/internal/v1/insights")
class RollupController(
    private val rollupService: RollupService,
) {

    @PostMapping("/rollup")
    fun rollupAll(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @RequestParam(defaultValue = "false") publish: Boolean,
    ): ResponseEntity<List<RollupDayResult>> {
        val results = rollupService.rollupAll(date, publish).map { it.toDto() }
        return ResponseEntity.ok(results)
    }

    @PostMapping("/rollup/{residentId}")
    fun rollupOne(
        @PathVariable residentId: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @RequestParam(defaultValue = "false") publish: Boolean,
    ): ResponseEntity<RollupDayResult> =
        ResponseEntity.ok(rollupService.rollupResident(residentId, date, publish).toDto())

    @PostMapping("/episodes/resolved")
    fun episodeResolved(@RequestBody body: EpisodeResolvedRequest): ResponseEntity<Map<String, Any>> {
        val recs = WellbeingRecommendations.forEpisodeResolved(body.selfRecovery, body.durationMinutes)
        return ResponseEntity.ok(
            mapOf(
                "residentId" to body.residentId,
                "episodeId" to (body.episodeId ?: ""),
                "recommendations" to recs,
            ),
        )
    }

    private fun com.hub.insights.rollup.RollupOutcome.toDto() = RollupDayResult(
        residentId = residentId,
        observedOn = observedOn.toString(),
        skipped = skipped,
        reason = reason,
        sleep = sleep,
        mobility = mobility,
        bathroom = bathroom,
        care = care,
        published = published,
    )
}
