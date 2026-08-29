package com.hub.views

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/views")
class ProjectionController(
    private val projectionService: ProjectionService
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

    @GetMapping("/resident-chart/{residentId}/sleep")
    fun getSleepTab(
        @PathVariable residentId: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): ResponseEntity<SleepTabProjection> {
        return ResponseEntity.ok(projectionService.getSleepTab(residentId, from, to))
    }

    @GetMapping("/resident-chart/{residentId}/mobility")
    fun getMobilityTab(
        @PathVariable residentId: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): ResponseEntity<MobilityTabProjection> {
        return ResponseEntity.ok(projectionService.getMobilityTab(residentId, from, to))
    }

    @GetMapping("/resident-chart/{residentId}/bathroom")
    fun getBathroomTab(
        @PathVariable residentId: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): ResponseEntity<BathroomTabProjection> {
        return ResponseEntity.ok(projectionService.getBathroomTab(residentId, from, to))
    }

    @GetMapping("/resident-chart/{residentId}/care")
    fun getCareTab(
        @PathVariable residentId: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): ResponseEntity<CareTabProjection> {
        return ResponseEntity.ok(projectionService.getCareTab(residentId, from, to))
    }

    @GetMapping("/resident-chart/{residentId}/falls")
    fun getFallsTab(
        @PathVariable residentId: String,
        @RequestParam(defaultValue = "12") months: Int,
    ): ResponseEntity<FallsTabProjection> {
        return ResponseEntity.ok(projectionService.getFallsTab(residentId, months))
    }

    @GetMapping("/resident-chart/{residentId}/episodes")
    fun getEpisodesTab(@PathVariable residentId: String): ResponseEntity<EpisodesTabProjection> {
        return ResponseEntity.ok(projectionService.getEpisodesTab(residentId))
    }
}
