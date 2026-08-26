package com.hub.residence.domain.model

import com.hub.shared.domain.AggregateRoot
import java.time.Instant

class Bed private constructor(
    override val id: BedId,
    val roomId: RoomId,
    val label: String,
    val monitorKey: String?,
    val retiredAt: Instant?,
    val retiredBy: String?,
    override var version: Long
) : AggregateRoot<BedId>() {

    val isRetired: Boolean get() = retiredAt != null

    fun retire(actorId: String): Bed {
        require(!isRetired) { "Bed is already retired" }
        return reconstitute(
            id = id, roomId = roomId, label = label, monitorKey = monitorKey,
            retiredAt = Instant.now(), retiredBy = actorId, version = version + 1
        )
    }

    fun updateProfile(label: String?, monitorKey: String?): Bed {
        return reconstitute(
            id = id, roomId = roomId, label = label ?: this.label,
            monitorKey = monitorKey ?: this.monitorKey,
            retiredAt = retiredAt, retiredBy = retiredBy, version = version + 1
        )
    }

    companion object {
        fun create(roomId: RoomId, label: String, monitorKey: String? = null): Bed = Bed(
            id = BedId.random(), roomId = roomId, label = label, monitorKey = monitorKey,
            retiredAt = null, retiredBy = null, version = 0
        )

        fun reconstitute(
            id: BedId, roomId: RoomId, label: String, monitorKey: String?,
            retiredAt: Instant?, retiredBy: String?, version: Long
        ): Bed = Bed(id, roomId, label, monitorKey, retiredAt, retiredBy, version)
    }
}
