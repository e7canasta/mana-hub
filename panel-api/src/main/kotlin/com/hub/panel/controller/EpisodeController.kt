package com.hub.panel.controller

import com.hub.panel.command.PanelCommandService
import com.hub.panel.projection.PanelProjectionService
import com.hub.panel.dto.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController("panelEpisodeController")
@RequestMapping("/api/v1/panel/episodes")
class PanelEpisodeController(
    private val projection: PanelProjectionService,
    private val command: PanelCommandService,
) {

    @GetMapping
    fun feed(): EpisodeFeedDto = projection.episodeFeed()

    @GetMapping("/{id}")
    fun detail(@PathVariable id: String): ResponseEntity<EpisodeDetailDto> =
        projection.episodeDetail(id)?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @PostMapping("/{id}/review")
    fun review(
        @PathVariable id: String,
        @RequestBody body: ReviewEpisodeRequest,
    ): ResponseEntity<ReviewEpisodeResponse> {
        val result = command.reviewEpisode(
            episodeId = id,
            verdict = body.verdict,
            note = body.note,
            actorId = body.actorId,
        )
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{id}/notes")
    fun notes(@PathVariable id: String): ResponseEntity<EpisodeNotesResponse> {
        return ResponseEntity.ok(EpisodeNotesResponse(notes = emptyList()))
    }

    @PostMapping("/{id}/notes")
    fun createNote(
        @PathVariable id: String,
        @RequestBody body: CreateEpisodeNoteRequest,
    ): ResponseEntity<NoteCreatedResponse> {
        val result = command.createNote(
            episodeId = id,
            kind = body.kind,
            body = body.body,
            authorId = body.authorId,
        )
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{id}/interventions")
    fun interventions(@PathVariable id: String): ResponseEntity<EpisodeInterventionsResponse> {
        return ResponseEntity.ok(EpisodeInterventionsResponse(interventions = emptyList()))
    }
}
