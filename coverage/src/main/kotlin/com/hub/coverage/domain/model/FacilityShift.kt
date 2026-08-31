package com.hub.coverage.domain.model

import com.hub.shared.domain.AggregateRoot
import com.hub.shared.domain.FacilityId
import java.time.Instant

class FacilityShift private constructor(
    override val id: StaffGroupId,
    val facilityId: FacilityId,
    val key: String,
    val label: String,
    val startMinute: Int,
    val sortOrder: Int,
    val retiredAt: Instant?,
    val retiredBy: String?,
    override var version: Long
) : AggregateRoot<StaffGroupId>() {

    init {
        require(startMinute in 0..1439) { "startMinute must be between 0 and 1439" }
    }

    val isRetired: Boolean get() = retiredAt != null

    fun retire(actorId: String): FacilityShift {
        require(!isRetired) { "FacilityShift is already retired" }
        return reconstitute(
            id = id, facilityId = facilityId, key = key, label = label,
            startMinute = startMinute, sortOrder = sortOrder,
            retiredAt = Instant.now(), retiredBy = actorId, version = version + 1
        )
    }

    companion object {
        fun create(facilityId: FacilityId, key: String, label: String, startMinute: Int, sortOrder: Int = 0): FacilityShift = FacilityShift(
            id = StaffGroupId.random(), facilityId = facilityId, key = key, label = label,
            startMinute = startMinute, sortOrder = sortOrder, retiredAt = null, retiredBy = null, version = 0
        )

        fun reconstitute(
            id: StaffGroupId, facilityId: FacilityId, key: String, label: String,
            startMinute: Int, sortOrder: Int, retiredAt: Instant?, retiredBy: String?, version: Long
        ): FacilityShift = FacilityShift(id, facilityId, key, label, startMinute, sortOrder, retiredAt, retiredBy, version)
    }
}
