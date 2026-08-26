package com.hub.history.api.rest

import com.hub.history.application.dto.*
import com.hub.history.application.service.IncidentApplicationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class IncidentController(
    private val incidentApplicationService: IncidentApplicationService
) {

    @GetMapping("/residents/{residentId}/incidents")
    fun getResidentIncidents(@PathVariable residentId: String): ResponseEntity<List<IncidentDetectionResponse>> {
        return ResponseEntity.ok(incidentApplicationService.getResidentIncidents(residentId))
    }

    @GetMapping("/incidents/{incidentId}/sequence")
    fun getIncidentSequence(@PathVariable incidentId: String): ResponseEntity<List<IncidentReviewResponse>> {
        return ResponseEntity.ok(incidentApplicationService.getIncidentSequence(incidentId))
    }

    @PatchMapping("/incidents/{incidentId}")
    fun reviewIncident(
        @PathVariable incidentId: String,
        @Valid @RequestBody request: ReviewIncidentRequest
    ): ResponseEntity<IncidentReviewResponse> {
        return ResponseEntity.ok(incidentApplicationService.reviewIncident(incidentId, request))
    }
}
