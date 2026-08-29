package com.hub.views

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/views")
class ResidentProjectionController(
    private val projectionService: ProjectionService,
) {

    @GetMapping("/resident-rail")
    fun getResidentRail(): ResponseEntity<List<ResidentRailItem>> {
        return ResponseEntity.ok(projectionService.getResidentRail())
    }

    @GetMapping("/resident-chart/{residentId}")
    fun getResidentChart(@PathVariable residentId: String): ResponseEntity<ResidentChartProjection> {
        return projectionService.getResidentChart(residentId)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }
}
