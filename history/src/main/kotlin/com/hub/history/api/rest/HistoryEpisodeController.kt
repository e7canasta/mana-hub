package com.hub.history.api.rest

import com.hub.history.application.dto.*
import com.hub.history.application.service.HistoryEpisodeApplicationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class HistoryEpisodeController(
    private val historyEpisodeApplicationService: HistoryEpisodeApplicationService
) {

    @PostMapping("/history-episodes")
    @ResponseStatus(HttpStatus.CREATED)
    fun ingestHistoryEpisode(
        @Valid @RequestBody request: IngestHistoryEpisodeRequest
    ): ResponseEntity<HistoryEpisodeResponse> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(historyEpisodeApplicationService.ingestHistoryEpisode(request))
    }

    @GetMapping("/residents/{residentId}/history-episodes")
    fun getResidentHistoryEpisodes(@PathVariable residentId: String): ResponseEntity<List<HistoryEpisodeResponse>> {
        return ResponseEntity.ok(historyEpisodeApplicationService.getResidentHistoryEpisodes(residentId))
    }

    @GetMapping("/history-episodes/{episodeId}")
    fun getHistoryEpisode(@PathVariable episodeId: String): ResponseEntity<HistoryEpisodeResponse> {
        val episode = historyEpisodeApplicationService.getHistoryEpisode(episodeId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(episode)
    }

    @GetMapping("/history-episodes/{episodeId}/sequence")
    fun getHistoryEpisodeSequence(@PathVariable episodeId: String): ResponseEntity<List<HistoryEpisodeReviewResponse>> {
        return ResponseEntity.ok(historyEpisodeApplicationService.getHistoryEpisodeSequence(episodeId))
    }

    @PatchMapping("/history-episodes/{episodeId}")
    fun reviewHistoryEpisode(
        @PathVariable episodeId: String,
        @Valid @RequestBody request: ReviewHistoryEpisodeRequest
    ): ResponseEntity<HistoryEpisodeReviewResponse> {
        return ResponseEntity.ok(historyEpisodeApplicationService.reviewHistoryEpisode(episodeId, request))
    }

    @GetMapping("/residents/{residentId}/falls")
    fun getFallsSummary(
        @PathVariable residentId: String,
        @RequestParam(defaultValue = "12") months: Int
    ): ResponseEntity<FallsSummaryResponse> {
        return ResponseEntity.ok(historyEpisodeApplicationService.getFallsSummary(residentId, months))
    }
}
