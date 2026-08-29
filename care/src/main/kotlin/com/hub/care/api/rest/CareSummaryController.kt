package com.hub.care.api.rest

import com.hub.care.application.dto.*
import com.hub.care.application.service.CareSummaryApplicationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
class CareSummaryController(
    private val careSummaryApplicationService: CareSummaryApplicationService
) {

    @GetMapping("/api/v1/residents/{residentId}/care")
    fun getCareSummary(
        @PathVariable residentId: String,
        @RequestParam from: LocalDate? = null,
        @RequestParam to: LocalDate? = null
    ): ResponseEntity<CareSummaryListResponse> {
        val toDate = to ?: LocalDate.now()
        val fromDate = from ?: toDate.minusDays(13)
        return ResponseEntity.ok(careSummaryApplicationService.getCareSummaryRange(residentId, fromDate, toDate))
    }

    @PostMapping("/internal/v1/care-summaries")
    fun ingestCareSummary(
        @RequestBody request: IngestCareSummaryRequest
    ): ResponseEntity<CareSummaryResponse> {
        return ResponseEntity.ok(careSummaryApplicationService.ingestCareSummary(request))
    }

    // Legacy alias — maintains backward compatibility for any client still using the old path
    @PostMapping("/api/v1/internal/care-summaries")
    fun ingestCareSummaryLegacy(
        @RequestBody request: IngestCareSummaryRequest
    ): ResponseEntity<CareSummaryResponse> = ingestCareSummary(request)
}
