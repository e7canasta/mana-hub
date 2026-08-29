package com.hub.views

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/views/resident-chart/{residentId}")
class WellbeingProjectionController(
    private val projectionService: ProjectionService,
) {

    @GetMapping("/sleep")
    fun getSleepTab(
        @PathVariable residentId: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): ResponseEntity<SleepTabProjection> {
        return ResponseEntity.ok(projectionService.getSleepTab(residentId, from, to))
    }

    @GetMapping("/mobility")
    fun getMobilityTab(
        @PathVariable residentId: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): ResponseEntity<MobilityTabProjection> {
        return ResponseEntity.ok(projectionService.getMobilityTab(residentId, from, to))
    }

    @GetMapping("/bathroom")
    fun getBathroomTab(
        @PathVariable residentId: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): ResponseEntity<BathroomTabProjection> {
        return ResponseEntity.ok(projectionService.getBathroomTab(residentId, from, to))
    }

    @GetMapping("/care")
    fun getCareTab(
        @PathVariable residentId: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): ResponseEntity<CareTabProjection> {
        return ResponseEntity.ok(projectionService.getCareTab(residentId, from, to))
    }

    @GetMapping("/falls")
    fun getFallsTab(
        @PathVariable residentId: String,
        @RequestParam(defaultValue = "12") months: Int,
    ): ResponseEntity<FallsTabProjection> {
        return ResponseEntity.ok(projectionService.getFallsTab(residentId, months))
    }
}
