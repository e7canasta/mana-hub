package com.hub.insights.api

import com.hub.insights.domain.find.FacilityBriefing
import com.hub.insights.domain.find.FacilityReport
import com.hub.insights.application.FindingService
import com.hub.insights.domain.find.ResidentBriefing
import com.hub.insights.domain.find.ResidentReport
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/insights")
class BriefingController(
    private val findings: FindingService,
) {

    @GetMapping("/resident-chart/{residentId}/briefing")
    fun residentBriefing(
        @PathVariable residentId: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
        @RequestParam(defaultValue = "14") days: Int,
    ): ResponseEntity<ResidentBriefing> {
        val (start, end) = findings.resolveWindow(from, to, days)
        val body = findings.residentBriefing(residentId, start, end) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(body)
    }

    @GetMapping("/resident-chart/{residentId}/report")
    fun residentReport(
        @PathVariable residentId: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
        @RequestParam(defaultValue = "30") days: Int,
    ): ResponseEntity<ResidentReport> {
        val (start, end) = findings.resolveWindow(from, to, days)
        val body = findings.residentReport(residentId, start, end) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(body)
    }

    @GetMapping("/facility/briefing")
    fun facilityBriefing(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
        @RequestParam(defaultValue = "14") days: Int,
    ): FacilityBriefing {
        val (start, end) = findings.resolveWindow(from, to, days)
        return findings.facilityBriefing(start, end)
    }

    @GetMapping("/facility/report")
    fun facilityReport(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
        @RequestParam(defaultValue = "30") days: Int,
    ): FacilityReport {
        val (start, end) = findings.resolveWindow(from, to, days)
        return findings.facilityReport(start, end)
    }
}
