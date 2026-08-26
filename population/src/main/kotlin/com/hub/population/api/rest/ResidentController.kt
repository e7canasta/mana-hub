package com.hub.population.api.rest

import com.hub.population.application.dto.*
import com.hub.population.application.service.ResidentApplicationService
import com.hub.population.domain.model.ResidentId
import com.hub.residence.domain.model.BedId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class ResidentController(
    private val residentApplicationService: ResidentApplicationService
) {

    @PostMapping("/residents")
    fun createResident(@Valid @RequestBody request: CreateResidentRequest): ResponseEntity<ResidentResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(residentApplicationService.createResident(request))
    }

    @GetMapping("/residents")
    fun listResidents(): ResponseEntity<List<ResidentResponse>> {
        return ResponseEntity.ok(residentApplicationService.listResidents())
    }

    @GetMapping("/residents/{residentId}")
    fun getResident(@PathVariable residentId: String): ResponseEntity<ResidentResponse> {
        return ResponseEntity.ok(residentApplicationService.getResident(ResidentId(residentId)))
    }

    @PatchMapping("/residents/{residentId}")
    fun updateResident(
        @PathVariable residentId: String,
        @Valid @RequestBody request: UpdateResidentRequest
    ): ResponseEntity<ResidentResponse> {
        return ResponseEntity.ok(residentApplicationService.updateResident(ResidentId(residentId), request))
    }

    @PostMapping("/residents/{residentId}/discharge")
    fun dischargeResident(
        @PathVariable residentId: String,
        @RequestBody(required = false) request: DischargeRequest?
    ): ResponseEntity<ResidentResponse> {
        return ResponseEntity.ok(residentApplicationService.dischargeResident(ResidentId(residentId), request?.actorId))
    }

    @PostMapping("/residents/{residentId}/assignments")
    fun createAssignment(
        @PathVariable residentId: String,
        @Valid @RequestBody request: CreateAssignmentRequest
    ): ResponseEntity<AssignmentResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            residentApplicationService.createAssignment(ResidentId(residentId), request)
        )
    }

    @GetMapping("/residents/{residentId}/assignments")
    fun getResidentAssignments(@PathVariable residentId: String): ResponseEntity<List<AssignmentResponse>> {
        return ResponseEntity.ok(residentApplicationService.getResidentAssignments(ResidentId(residentId)))
    }

    @GetMapping("/assignments/open")
    fun listOpenAssignments(): ResponseEntity<List<OpenAssignmentResponse>> {
        return ResponseEntity.ok(residentApplicationService.listOpenAssignments())
    }

    @DeleteMapping("/beds/{bedId}/assignment")
    fun deleteAssignment(@PathVariable bedId: String): ResponseEntity<Void> {
        residentApplicationService.deleteAssignment(BedId(bedId))
        return ResponseEntity.noContent().build()
    }
}
