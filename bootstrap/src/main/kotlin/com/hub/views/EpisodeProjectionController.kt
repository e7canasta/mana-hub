package com.hub.views

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/views/resident-chart/{residentId}")
class EpisodeProjectionController(
    private val projectionService: ProjectionService,
) {

    @GetMapping("/episodes")
    fun getEpisodesTab(@PathVariable residentId: String): ResponseEntity<EpisodesTabProjection> {
        return ResponseEntity.ok(projectionService.getEpisodesTab(residentId))
    }
}
