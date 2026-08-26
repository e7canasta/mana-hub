package com.hub.streams.domain.model

import com.hub.shared.domain.AggregateRoot
import com.hub.residence.domain.model.RoomId
import java.time.Instant

class Stream private constructor(
    override val id: StreamId,
    val roomId: RoomId,
    val streamKey: String,
    val name: String?,
    val active: Boolean,
    override var version: Long
) : AggregateRoot<StreamId>() {

    fun deactivate(): Stream = reconstitute(
        id = id, roomId = roomId, streamKey = streamKey, name = name,
        active = false, version = version + 1
    )

    fun activate(): Stream = reconstitute(
        id = id, roomId = roomId, streamKey = streamKey, name = name,
        active = true, version = version + 1
    )

    companion object {
        fun create(roomId: RoomId, streamKey: String, name: String?): Stream = Stream(
            id = StreamId.random(), roomId = roomId, streamKey = streamKey, name = name, active = true, version = 0
        )

        fun reconstitute(id: StreamId, roomId: RoomId, streamKey: String, name: String?, active: Boolean, version: Long): Stream =
            Stream(id, roomId, streamKey, name, active, version)
    }
}
