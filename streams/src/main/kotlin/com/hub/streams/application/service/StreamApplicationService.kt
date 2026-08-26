package com.hub.streams.application.service

import com.hub.streams.application.dto.*
import com.hub.streams.domain.model.*
import com.hub.streams.domain.repository.StreamRegionRepository
import com.hub.streams.domain.repository.StreamRepository
import com.hub.residence.domain.model.RoomId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StreamApplicationService(
    private val streamRepository: StreamRepository,
    private val regionRepository: StreamRegionRepository
) {

    @Transactional
    fun createStream(roomId: String, request: CreateStreamRequest): StreamResponse {
        val stream = Stream.create(RoomId(roomId), request.streamKey, request.name)
        return streamRepository.save(stream).toResponse()
    }

    @Transactional(readOnly = true)
    fun listStreams(roomId: String): List<StreamResponse> {
        return streamRepository.findByRoomId(RoomId(roomId)).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getStream(id: String): StreamResponse {
        return streamRepository.findById(StreamId(id))?.toResponse()
            ?: throw IllegalArgumentException("Stream not found: $id")
    }

    @Transactional(readOnly = true)
    fun listRegions(streamId: String): List<StreamRegionResponse> {
        return regionRepository.findByStreamId(StreamId(streamId)).map { it.toResponse() }
    }

    @Transactional
    fun replaceRegions(streamId: String, request: ReplaceRegionsRequest): List<StreamRegionResponse> {
        val streamIdObj = StreamId(streamId)
        regionRepository.deleteByStreamId(streamIdObj)
        return request.regions.map { req ->
            val region = StreamRegion.create(streamIdObj, req.regionType, req.points, req.label)
            regionRepository.save(region).toResponse()
        }
    }

    @Transactional
    fun updateRegion(streamId: String, regionId: String, request: UpdateRegionRequest): StreamRegionResponse {
        val region = regionRepository.findById(StreamId(regionId))
            ?: throw IllegalArgumentException("Region not found: $regionId")
        val updated = region.updatePoints(request.points ?: region.points, "system")
        return regionRepository.save(updated).toResponse()
    }

    private fun Stream.toResponse() = StreamResponse(
        id = id.value, roomId = roomId.value, streamKey = streamKey, name = name, active = active
    )

    private fun StreamRegion.toResponse() = StreamRegionResponse(
        id = id.value, streamId = streamId.value, regionType = regionType, points = points,
        label = label, isStatic = isStatic
    )
}
