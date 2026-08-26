package com.hub.observation.api.rest

import com.hub.observation.application.dto.*
import com.hub.observation.application.service.ObservationApplicationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1")
class ObservationController(
    private val observationApplicationService: ObservationApplicationService
) {

    @GetMapping("/wings/{wingId}/board")
    fun getWingBoard(@PathVariable wingId: String): ResponseEntity<List<BedStateResponse>> {
        return ResponseEntity.ok(emptyList())
    }

    @GetMapping("/rooms/{roomId}/peek")
    fun peekRoom(@PathVariable roomId: String): ResponseEntity<Any> {
        return ResponseEntity.ok(mapOf("roomId" to roomId))
    }

    @GetMapping("/residents/{residentId}/sleep")
    fun getSleepSummary(
        @PathVariable residentId: String,
        @RequestParam(defaultValue = "#{T(java.time.LocalDate).now()}") date: LocalDate
    ): ResponseEntity<SleepSummaryResponse> {
        val summary = observationApplicationService.getSleepSummary(residentId, date)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(summary)
    }

    @GetMapping("/residents/{residentId}/mobility")
    fun getMobilitySummary(
        @PathVariable residentId: String,
        @RequestParam(defaultValue = "#{T(java.time.LocalDate).now()}") date: LocalDate
    ): ResponseEntity<MobilitySummaryResponse> {
        val summary = observationApplicationService.getMobilitySummary(residentId, date)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(summary)
    }

    @GetMapping("/residents/{residentId}/bathroom")
    fun getBathroomSummary(
        @PathVariable residentId: String,
        @RequestParam(defaultValue = "#{T(java.time.LocalDate).now()}") date: LocalDate
    ): ResponseEntity<BathroomSummaryResponse> {
        val summary = observationApplicationService.getBathroomSummary(residentId, date)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(summary)
    }

    @GetMapping("/residents/{residentId}/current-state")
    fun getCurrentState(@PathVariable residentId: String): ResponseEntity<Any> {
        return ResponseEntity.ok(mapOf("residentId" to residentId))
    }

    @GetMapping("/residents/{residentId}/timeline")
    fun getTimeline(@PathVariable residentId: String): ResponseEntity<Any> {
        return ResponseEntity.ok(mapOf("residentId" to residentId))
    }

    @GetMapping("/residents/{residentId}/events")
    fun getEvents(@PathVariable residentId: String): ResponseEntity<Any> {
        return ResponseEntity.ok(mapOf("residentId" to residentId))
    }

    @GetMapping("/residents/{residentId}/notifications")
    fun getNotificationsByResident(@PathVariable residentId: String): ResponseEntity<List<NotificationResponse>> {
        return ResponseEntity.ok(observationApplicationService.getNotificationsByResident(residentId))
    }

    @GetMapping("/beds/{bedId}/notifications")
    fun getNotificationsByBed(@PathVariable bedId: String): ResponseEntity<List<NotificationResponse>> {
        return ResponseEntity.ok(observationApplicationService.getNotificationsByBed(bedId))
    }

    @GetMapping("/companion/rooms")
    fun getCompanionRooms(): ResponseEntity<Any> {
        return ResponseEntity.ok(emptyList<Any>())
    }

    @GetMapping("/reports/summary")
    fun getReportsSummary(): ResponseEntity<Any> {
        return ResponseEntity.ok(mapOf("summary" to "ok"))
    }
}
