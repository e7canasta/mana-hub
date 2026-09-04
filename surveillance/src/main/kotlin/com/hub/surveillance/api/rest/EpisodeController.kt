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
    @ResponseStatus(HttpStatus.CREATED)
    fun createEpisode(@Valid @RequestBody request: CreateEpisodeRequest): EpisodeResponse =
        episodeApplicationService.createEpisode(request)

    @GetMapping
    fun listEpisodes(
        @RequestParam residentId: String? = null,
        @RequestParam status: String? = null,
        @RequestParam from: String? = null,
        @RequestParam to: String? = null
    ): List<EpisodeResponse> =
        episodeApplicationService.listEpisodes(residentId, status, from, to)

    @GetMapping("/{episodeId}")
    fun getEpisode(@PathVariable episodeId: String): ResponseEntity<EpisodeResponse> =
        episodeApplicationService.getEpisode(episodeId)?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @PostMapping("/{episodeId}/acknowledge")
    fun acknowledgeEpisode(
        @PathVariable episodeId: String,
        @Valid @RequestBody request: AcknowledgeEpisodeRequest
    ): EpisodeResponse =
        episodeApplicationService.acknowledgeEpisode(episodeId, request.actorId)

    @PostMapping("/{episodeId}/resolved")
    fun resolveEpisode(
        @PathVariable episodeId: String,
        @Valid @RequestBody request: ResolveEpisodeRequest
    ): EpisodeResponse =
        episodeApplicationService.resolveEpisode(episodeId, request.staffMemberId)

    @PatchMapping("/{episodeId}")
    fun updateEpisode(
        @PathVariable episodeId: String,
        @Valid @RequestBody request: UpdateEpisodeRequest
    ): EpisodeResponse =
        episodeApplicationService.updateEpisode(episodeId, request)
}
