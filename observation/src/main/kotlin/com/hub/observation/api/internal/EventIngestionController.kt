package com.hub.observation.api.internal

import com.hub.observation.application.dto.*
import com.hub.observation.application.service.ObservationApplicationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/internal/v1")
class EventIngestionController(
    private val observationApplicationService: ObservationApplicationService
) {

    @PostMapping("/events")
    fun ingestEvent(@RequestBody request: IngestEventRequest): ResponseEntity<Void> {
        observationApplicationService.ingestEvent(request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/clinical/sleep-summaries")
    fun ingestSleepSummary(@RequestBody request: IngestSummaryRequest<SleepSummaryData>): ResponseEntity<Void> {
        observationApplicationService.ingestSleepSummary(request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/clinical/mobility-summaries")
    fun ingestMobilitySummary(@RequestBody request: IngestSummaryRequest<MobilitySummaryData>): ResponseEntity<Void> {
        observationApplicationService.ingestMobilitySummary(request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/clinical/bathroom-summaries")
    fun ingestBathroomSummary(@RequestBody request: IngestSummaryRequest<BathroomSummaryData>): ResponseEntity<Void> {
        observationApplicationService.ingestBathroomSummary(request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/clinical/incidents")
    fun ingestIncident(@RequestBody request: Any): ResponseEntity<Void> {
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/notifications")
    fun ingestNotification(@RequestBody request: IngestNotificationRequest): ResponseEntity<Void> {
        observationApplicationService.ingestNotification(request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}
