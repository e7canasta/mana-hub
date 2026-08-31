package com.hub.coverage.domain.model

import com.hub.shared.domain.AggregateRoot
import com.hub.shared.domain.FacilityId
import java.time.Instant

class StaffGroup private constructor(
    override val id: StaffGroupId,
    val facilityId: FacilityId,
    val name: String,
    val retiredAt: Instant?,
    val retiredBy: String?,
    override var version: Long
) : AggregateRoot<StaffGroupId>() {

    val isRetired: Boolean get() = retiredAt != null

    fun retire(actorId: String): StaffGroup {
        require(!isRetired) { "StaffGroup is already retired" }
        return reconstitute(
            id = id, facilityId = facilityId, name = name,
            retiredAt = Instant.now(), retiredBy = actorId, version = version + 1
        )
    }

    companion object {
        fun create(facilityId: FacilityId, name: String): StaffGroup = StaffGroup(
            id = StaffGroupId.random(), facilityId = facilityId, name = name,
            retiredAt = null, retiredBy = null, version = 0
        )

        fun reconstitute(
            id: StaffGroupId, facilityId: FacilityId, name: String,
            retiredAt: Instant?, retiredBy: String?, version: Long
        ): StaffGroup = StaffGroup(id, facilityId, name, retiredAt, retiredBy, version)
    }
}
