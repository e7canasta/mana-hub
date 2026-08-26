package com.hub.surveillance.api.rest

import com.hub.surveillance.application.dto.*
import com.hub.surveillance.application.service.EpisodeApplicationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/episodes")
class EpisodeController(
    private val episodeApplicationService: EpisodeApplicationService
) {

    @PostMapping
    fun createEpisode(@Valid @RequestBody request: CreateEpisodeRequest): ResponseEntity<EpisodeResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(episodeApplicationService.createEpisode(request))
    }

    @GetMapping
    fun listEpisodes(): ResponseEntity<List<EpisodeResponse>> {
        return ResponseEntity.ok(episodeApplicationService.listEpisodes())
    }

    @GetMapping("/{episodeId}")
    fun getEpisode(@PathVariable episodeId: String): ResponseEntity<EpisodeResponse> {
        val episode = episodeApplicationService.getEpisode(episodeId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(episode)
    }

    @PostMapping("/{episodeId}/acknowledge")
    fun acknowledgeEpisode(
        @PathVariable episodeId: String,
        @Valid @RequestBody request: AcknowledgeEpisodeRequest
    ): ResponseEntity<EpisodeResponse> {
        return ResponseEntity.ok(episodeApplicationService.acknowledgeEpisode(episodeId, request.actorId))
    }

    @PatchMapping("/{episodeId}")
    fun updateEpisode(
        @PathVariable episodeId: String,
        @Valid @RequestBody request: UpdateEpisodeRequest
    ): ResponseEntity<EpisodeResponse> {
        return ResponseEntity.ok(episodeApplicationService.updateEpisode(episodeId, request))
    }
}
