package com.hub.history.api.rest

import com.hub.history.application.dto.EpisodeTimelineDto
import com.hub.history.application.dto.EpisodeTimelineEventDto
import com.hub.history.application.service.EpisodeTimelineService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class EpisodeTimelineController(
    private val service: EpisodeTimelineService,
) {

    @GetMapping("/api/v1/history-episodes/{episodeId}/timeline")
    fun getTimeline(
        @PathVariable episodeId: String,
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "100") limit: Int,
    ): ResponseEntity<EpisodeTimelineDto> {
        val timeline = service.getTimelineByEpisode(episodeId, offset, limit)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(timeline.toDto())
    }

    @GetMapping("/api/v1/residents/{residentId}/episode-timeline")
    fun getResidentTimeline(
        @PathVariable residentId: String,
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "100") limit: Int,
    ): ResponseEntity<List<EpisodeTimelineEventDto>> {
        val events = service.getEventsByResident(residentId, offset, limit)
        return ResponseEntity.ok(events.map { it.toDto() })
    }

    private fun com.hub.history.domain.model.timeline.EpisodeTimeline.toDto() = EpisodeTimelineDto(
        episodeId = episodeId,
        residentId = residentId.value,
        events = events.map { it.toDto() },
    )

    private fun com.hub.history.domain.model.timeline.EpisodeTimelineEvent.toDto() = EpisodeTimelineEventDto(
        id = id.value,
        at = at.toString(),
        type = type.name,
        fromState = fromState,
        toState = toState,
        description = description,
    )
}
