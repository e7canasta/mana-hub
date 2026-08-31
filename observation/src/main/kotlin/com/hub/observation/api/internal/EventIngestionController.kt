package com.hub.observation.api.internal

import com.hub.observation.application.dto.*
import com.hub.observation.application.service.EventIngestionService
import com.hub.observation.application.service.ObservationApplicationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/internal/v1")
class EventIngestionController(
    private val eventIngestionService: EventIngestionService,
    private val observationApplicationService: ObservationApplicationService
) {

    @PostMapping("/events")
    fun ingestEvent(@RequestBody request: IngestEventRequest): ResponseEntity<Void> {
        eventIngestionService.ingestEvent(request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/clinical/sleep-summaries")
    fun ingestSleepSummary(@RequestBody request: IngestSummaryRequest<SleepSummaryData>): ResponseEntity<Void> {
        val created = eventIngestionService.ingestSleepSummary(request)
        return if (created) ResponseEntity.status(HttpStatus.CREATED).build() else ResponseEntity.ok().build()
    }

    @PostMapping("/clinical/mobility-summaries")
    fun ingestMobilitySummary(@RequestBody request: IngestSummaryRequest<MobilitySummaryData>): ResponseEntity<Void> {
        val created = eventIngestionService.ingestMobilitySummary(request)
        return if (created) ResponseEntity.status(HttpStatus.CREATED).build() else ResponseEntity.ok().build()
    }

    @PostMapping("/clinical/bathroom-summaries")
    fun ingestBathroomSummary(@RequestBody request: IngestSummaryRequest<BathroomSummaryData>): ResponseEntity<Void> {
        val created = eventIngestionService.ingestBathroomSummary(request)
        return if (created) ResponseEntity.status(HttpStatus.CREATED).build() else ResponseEntity.ok().build()
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

    @PostMapping("/scene-events")
    fun ingestSceneEvent(@RequestBody request: IngestSceneEventRequest): ResponseEntity<Void> {
        eventIngestionService.ingestSceneEvent(request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}
