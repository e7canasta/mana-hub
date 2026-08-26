package com.hub.evidence.api.rest

import com.hub.evidence.application.dto.*
import com.hub.evidence.application.service.EvidenceApplicationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class EvidenceController(
    private val evidenceApplicationService: EvidenceApplicationService
) {

    @PostMapping("/evidence")
    fun createEvidence(@Valid @RequestBody request: CreateEvidenceRequest): ResponseEntity<EvidenceResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(evidenceApplicationService.createEvidence(request))
    }

    @PostMapping("/timelines")
    fun createTimeline(@RequestParam bedId: String, @RequestParam residentId: String): ResponseEntity<TimelineResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(evidenceApplicationService.createTimeline(bedId, residentId))
    }

    @PostMapping("/timelines/{timelineId}/close")
    fun closeTimeline(@PathVariable timelineId: String): ResponseEntity<TimelineResponse> {
        return ResponseEntity.ok(evidenceApplicationService.closeTimeline(timelineId))
    }

    @PostMapping("/clip-windows")
    fun createClipWindow(@RequestParam bedId: String, @RequestParam residentId: String): ResponseEntity<ClipWindowResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(evidenceApplicationService.createClipWindow(bedId, residentId))
    }

    @PostMapping("/clip-windows/{windowId}/close")
    fun closeClipWindow(@PathVariable windowId: String): ResponseEntity<ClipWindowResponse> {
        return ResponseEntity.ok(evidenceApplicationService.closeClipWindow(windowId))
    }

    @GetMapping("/clip-windows/{bedId}/open")
    fun getOpenClipWindows(@PathVariable bedId: String): ResponseEntity<List<ClipWindowResponse>> {
        return ResponseEntity.ok(evidenceApplicationService.getOpenClipWindows(bedId))
    }
}
