package com.hub.coverage.application.dto

import com.hub.coverage.domain.model.StaffGroupId

data class CreateStaffGroupRequest(
    val name: String
)

data class StaffGroupResponse(
    val id: String,
    val facilityId: String,
    val name: String,
    val isRetired: Boolean
)

data class CreateShiftRequest(
    val key: String,
    val label: String,
    val startMinute: Int,
    val sortOrder: Int = 0
)

data class ShiftResponse(
    val id: String,
    val facilityId: String,
    val key: String,
    val label: String,
    val startMinute: Int,
    val sortOrder: Int,
    val isRetired: Boolean
)

data class UpdateStaffGroupMembersRequest(
    val memberIds: List<String>
)
