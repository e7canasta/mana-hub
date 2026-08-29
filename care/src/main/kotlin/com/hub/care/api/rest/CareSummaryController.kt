package com.hub.care.api.rest

import com.hub.care.application.dto.*
import com.hub.care.application.service.CareSummaryApplicationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1")
class CareSummaryController(
    private val careSummaryApplicationService: CareSummaryApplicationService
) {

    @GetMapping("/residents/{residentId}/care")
    fun getCareSummary(
        @PathVariable residentId: String,
        @RequestParam from: LocalDate? = null,
        @RequestParam to: LocalDate? = null
    ): ResponseEntity<CareSummaryListResponse> {
        val toDate = to ?: LocalDate.now()
        val fromDate = from ?: toDate.minusDays(13)
        return ResponseEntity.ok(careSummaryApplicationService.getCareSummaryRange(residentId, fromDate, toDate))
    }

    @PostMapping("/internal/care-summaries")
    fun ingestCareSummary(
        @RequestBody request: IngestCareSummaryRequest
    ): ResponseEntity<CareSummaryResponse> {
        return ResponseEntity.ok(careSummaryApplicationService.ingestCareSummary(request))
    }
}
