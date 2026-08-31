package com.hub.coverage.domain.model

import com.hub.shared.domain.AggregateRoot
import com.hub.shared.domain.FacilityId
import com.hub.shared.domain.StaffMemberId
import com.hub.shared.domain.UserId

class StaffMember private constructor(
    override val id: StaffMemberId,
    val facilityId: FacilityId,
    val fullName: String,
    val role: StaffRole,
    val userId: UserId?,
    override var version: Long
) : AggregateRoot<StaffMemberId>() {

    val hasUserAccount: Boolean get() = userId != null

    companion object {
        fun create(
            facilityId: FacilityId,
            fullName: String,
            role: StaffRole,
            userId: UserId? = null
        ): StaffMember = StaffMember(
            id = StaffMemberId.random(),
            facilityId = facilityId,
            fullName = fullName,
            role = role,
            userId = userId,
            version = 0
        )

        fun reconstitute(
            id: StaffMemberId,
            facilityId: FacilityId,
            fullName: String,
            role: StaffRole,
            userId: UserId?,
            version: Long
        ): StaffMember = StaffMember(id, facilityId, fullName, role, userId, version)
    }
}
