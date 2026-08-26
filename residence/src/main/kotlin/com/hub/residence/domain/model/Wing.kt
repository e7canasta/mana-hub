package com.hub.residence.domain.model

import com.hub.shared.domain.AggregateRoot
import java.time.Instant

class Wing private constructor(
    override val id: WingId,
    val facilityId: FacilityId,
    val name: String,
    val floor: String?,
    val sortOrder: Int,
    val retiredAt: Instant?,
    val retiredBy: String?,
    override var version: Long
) : AggregateRoot<WingId>() {

    val isRetired: Boolean get() = retiredAt != null

    fun retire(actorId: String): Wing {
        require(!isRetired) { "Wing is already retired" }
        return reconstitute(
            id = id, facilityId = facilityId, name = name, floor = floor,
            sortOrder = sortOrder, retiredAt = Instant.now(), retiredBy = actorId,
            version = version + 1
        )
    }

    fun updateProfile(name: String?, floor: String?, sortOrder: Int?): Wing {
        return reconstitute(
            id = id, facilityId = facilityId, name = name ?: this.name,
            floor = floor ?: this.floor, sortOrder = sortOrder ?: this.sortOrder,
            retiredAt = retiredAt, retiredBy = retiredBy, version = version + 1
        )
    }

    companion object {
        fun create(facilityId: FacilityId, name: String, floor: String? = null, sortOrder: Int = 0): Wing = Wing(
            id = WingId.random(), facilityId = facilityId, name = name, floor = floor,
            sortOrder = sortOrder, retiredAt = null, retiredBy = null, version = 0
        )

        fun reconstitute(
            id: WingId, facilityId: FacilityId, name: String, floor: String?,
            sortOrder: Int, retiredAt: Instant?, retiredBy: String?, version: Long
        ): Wing = Wing(id, facilityId, name, floor, sortOrder, retiredAt, retiredBy, version)
    }
}
