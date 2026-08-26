package com.hub.streams.domain.model

import com.hub.shared.domain.AggregateRoot
import java.time.Instant

class StreamRegion private constructor(
    override val id: StreamId,
    val streamId: StreamId,
    val regionType: RegionType,
    val points: String,
    val label: String?,
    val isStatic: Boolean,
    val updatedBy: String?,
    override var version: Long
) : AggregateRoot<StreamId>() {

    fun updatePoints(points: String, updatedBy: String): StreamRegion =
        reconstitute(
            id = id, streamId = streamId, regionType = regionType, points = points,
            label = label, isStatic = isStatic, updatedBy = updatedBy, version = version + 1
        )

    companion object {
        fun create(streamId: StreamId, regionType: RegionType, points: String, label: String?): StreamRegion = StreamRegion(
            id = StreamId.random(), streamId = streamId, regionType = regionType, points = points,
            label = label, isStatic = true, updatedBy = null, version = 0
        )

        fun reconstitute(
            id: StreamId, streamId: StreamId, regionType: RegionType, points: String,
            label: String?, isStatic: Boolean, updatedBy: String?, version: Long
        ): StreamRegion = StreamRegion(id, streamId, regionType, points, label, isStatic, updatedBy, version)
    }
}
