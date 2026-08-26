package com.hub.residence.domain.model

import com.hub.shared.domain.AggregateRoot
import java.time.Instant

class Room private constructor(
    override val id: RoomId,
    val wingId: WingId,
    val number: String,
    val roomType: String?,
    val streamKey: String?,
    val retiredAt: Instant?,
    val retiredBy: String?,
    override var version: Long
) : AggregateRoot<RoomId>() {

    val isRetired: Boolean get() = retiredAt != null

    fun retire(actorId: String): Room {
        require(!isRetired) { "Room is already retired" }
        return reconstitute(
            id = id, wingId = wingId, number = number, roomType = roomType,
            streamKey = streamKey, retiredAt = Instant.now(), retiredBy = actorId,
            version = version + 1
        )
    }

    fun updateProfile(number: String?, roomType: String?, streamKey: String?): Room {
        return reconstitute(
            id = id, wingId = wingId, number = number ?: this.number,
            roomType = roomType ?: this.roomType, streamKey = streamKey ?: this.streamKey,
            retiredAt = retiredAt, retiredBy = retiredBy, version = version + 1
        )
    }

    companion object {
        fun create(wingId: WingId, number: String, roomType: String? = null, streamKey: String? = null): Room = Room(
            id = RoomId.random(), wingId = wingId, number = number, roomType = roomType,
            streamKey = streamKey, retiredAt = null, retiredBy = null, version = 0
        )

        fun reconstitute(
            id: RoomId, wingId: WingId, number: String, roomType: String?,
            streamKey: String?, retiredAt: Instant?, retiredBy: String?, version: Long
        ): Room = Room(id, wingId, number, roomType, streamKey, retiredAt, retiredBy, version)
    }
}
