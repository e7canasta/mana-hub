package com.hub.coverage.api.rest

import com.hub.coverage.application.dto.*
import com.hub.coverage.application.service.StaffGroupApplicationService
import com.hub.coverage.domain.model.StaffGroupId
import com.hub.shared.domain.FacilityId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class StaffGroupController(
    private val staffGroupApplicationService: StaffGroupApplicationService
) {

    @PostMapping("/facilities/{facilityId}/staff-groups")
    fun createStaffGroup(
        @PathVariable facilityId: String,
        @Valid @RequestBody request: CreateStaffGroupRequest
    ): ResponseEntity<StaffGroupResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            staffGroupApplicationService.createStaffGroup(FacilityId(facilityId), request)
        )
    }

    @GetMapping("/staff-groups")
    fun listStaffGroups(@RequestParam facilityId: String?): ResponseEntity<List<StaffGroupResponse>> {
        return ResponseEntity.ok(staffGroupApplicationService.listStaffGroups(FacilityId(facilityId ?: "")))
    }

    @GetMapping("/staff-groups/{groupId}")
    fun getStaffGroup(@PathVariable groupId: String): ResponseEntity<StaffGroupResponse> {
        return ResponseEntity.ok(staffGroupApplicationService.getStaffGroup(StaffGroupId(groupId)))
    }

    @PostMapping("/facilities/{facilityId}/shifts")
    fun createShift(
        @PathVariable facilityId: String,
        @Valid @RequestBody request: CreateShiftRequest
    ): ResponseEntity<ShiftResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            staffGroupApplicationService.createShift(FacilityId(facilityId), request)
        )
    }

    @GetMapping("/facilities/{facilityId}/shifts")
    fun listShifts(@PathVariable facilityId: String): ResponseEntity<List<ShiftResponse>> {
        return ResponseEntity.ok(staffGroupApplicationService.listShifts(FacilityId(facilityId)))
    }
}
