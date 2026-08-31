package com.hub.coverage.domain.repository

import com.hub.coverage.domain.model.*
import com.hub.shared.domain.FacilityId
import com.hub.shared.domain.StaffMemberId

interface StaffGroupRepository {
    fun findById(id: StaffGroupId): StaffGroup?
    fun findByFacilityId(facilityId: FacilityId): List<StaffGroup>
    fun save(staffGroup: StaffGroup): StaffGroup
}

interface ShiftRepository {
    fun findById(id: StaffGroupId): FacilityShift?
    fun findByFacilityId(facilityId: FacilityId): List<FacilityShift>
    fun save(shift: FacilityShift): FacilityShift
}

interface StaffMemberRepository {
    fun findById(id: StaffMemberId): StaffMember?
    fun findByFacilityId(facilityId: String): List<StaffMember>
    fun findByUserId(userId: String): StaffMember?
    fun save(member: StaffMember): StaffMember
}
