package com.hub.coverage.application.service

import com.hub.coverage.application.dto.*
import com.hub.coverage.domain.model.*
import com.hub.coverage.domain.repository.ShiftRepository
import com.hub.coverage.domain.repository.StaffGroupRepository
import com.hub.shared.domain.FacilityId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StaffGroupApplicationService(
    private val staffGroupRepository: StaffGroupRepository,
    private val shiftRepository: ShiftRepository
) {

    @Transactional
    fun createStaffGroup(facilityId: FacilityId, request: CreateStaffGroupRequest): StaffGroupResponse {
        val group = StaffGroup.create(facilityId = facilityId, name = request.name)
        return staffGroupRepository.save(group).toResponse()
    }

    @Transactional(readOnly = true)
    fun getStaffGroup(id: StaffGroupId): StaffGroupResponse {
        return staffGroupRepository.findById(id)?.toResponse()
            ?: throw IllegalArgumentException("StaffGroup not found: $id")
    }

    @Transactional(readOnly = true)
    fun listStaffGroups(facilityId: FacilityId): List<StaffGroupResponse> {
        return staffGroupRepository.findByFacilityId(facilityId).map { it.toResponse() }
    }

    @Transactional
    fun createShift(facilityId: FacilityId, request: CreateShiftRequest): ShiftResponse {
        val shift = FacilityShift.create(
            facilityId = facilityId, key = request.key, label = request.label,
            startMinute = request.startMinute, sortOrder = request.sortOrder
        )
        return shiftRepository.save(shift).toResponse()
    }

    @Transactional(readOnly = true)
    fun listShifts(facilityId: FacilityId): List<ShiftResponse> {
        return shiftRepository.findByFacilityId(facilityId).map { it.toResponse() }
    }

    private fun StaffGroup.toResponse() = StaffGroupResponse(
        id = id.value, facilityId = facilityId.value, name = name, isRetired = isRetired
    )

    private fun FacilityShift.toResponse() = ShiftResponse(
        id = id.value, facilityId = facilityId.value, key = key, label = label,
        startMinute = startMinute, sortOrder = sortOrder, isRetired = isRetired
    )
}
