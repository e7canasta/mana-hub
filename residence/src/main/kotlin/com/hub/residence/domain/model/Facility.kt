package com.hub.residence.domain.model

import com.hub.shared.domain.AggregateRoot
import com.hub.shared.domain.FacilityId
import java.time.Instant

class Facility private constructor(
    override val id: FacilityId,
    val name: String,
    val timezone: String,
    val retiredAt: Instant?,
    val retiredBy: String?,
    override var version: Long
) : AggregateRoot<FacilityId>() {

    val isRetired: Boolean get() = retiredAt != null

    fun retire(actorId: String): Facility {
        require(!isRetired) { "Facility is already retired" }
        return reconstitute(
            id = id, name = name, timezone = timezone,
            retiredAt = Instant.now(), retiredBy = actorId, version = version + 1
        )
    }

    fun updateProfile(name: String?, timezone: String?): Facility {
        return reconstitute(
            id = id, name = name ?: this.name, timezone = timezone ?: this.timezone,
            retiredAt = retiredAt, retiredBy = retiredBy, version = version + 1
        )
    }

    companion object {
        fun create(name: String, timezone: String = "UTC"): Facility = Facility(
            id = FacilityId.random(), name = name, timezone = timezone,
            retiredAt = null, retiredBy = null, version = 0
        )

        fun reconstitute(
            id: FacilityId, name: String, timezone: String,
            retiredAt: Instant?, retiredBy: String?, version: Long
        ): Facility = Facility(id, name, timezone, retiredAt, retiredBy, version)
    }
}
