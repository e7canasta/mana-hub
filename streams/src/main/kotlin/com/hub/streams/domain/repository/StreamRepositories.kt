package com.hub.streams.domain.repository

import com.hub.streams.domain.model.*
import com.hub.residence.domain.model.RoomId

interface StreamRepository {
    fun findById(id: StreamId): Stream?
    fun findByRoomId(roomId: RoomId): List<Stream>
    fun save(stream: Stream): Stream
}

interface StreamRegionRepository {
    fun findById(id: StreamId): StreamRegion?
    fun findByStreamId(streamId: StreamId): List<StreamRegion>
    fun save(region: StreamRegion): StreamRegion
    fun deleteByStreamId(streamId: StreamId)
}
