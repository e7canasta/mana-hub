package com.hub.streams.infrastructure.persistence

import com.hub.streams.domain.model.*
import com.hub.streams.domain.repository.StreamRegionRepository
import com.hub.streams.domain.repository.StreamRepository
import com.hub.shared.domain.RoomId
import com.hub.shared.time.HubClock
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Entity
@Table(name = "streams")
class StreamEntity(
    @Id var id: String = "",
    @Column(name = "room_id") var roomId: String = "",
    @Column(name = "stream_key") var streamKey: String = "",
    @Column(name = "name") var name: String? = null,
    @Column(name = "active") var active: Boolean = true,
    @Column(name = "created_at") var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at") var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0
)

@Entity
@Table(name = "stream_regions")
class StreamRegionEntity(
    @Id var id: String = "",
    @Column(name = "stream_id") var streamId: String = "",
    @Column(name = "region_type") var regionType: String = "",
    @Column(name = "points") var points: String = "",
    @Column(name = "label") var label: String? = null,
    @Column(name = "is_static") var isStatic: Boolean = true,
    @Column(name = "updated_by") var updatedBy: String? = null,
    @Column(name = "created_at") var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at") var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0
)

@Repository
interface StreamEntityRepository : JpaRepository<StreamEntity, String> {
    fun findByRoomId(roomId: String): List<StreamEntity>
}

@Repository
interface StreamRegionEntityRepository : JpaRepository<StreamRegionEntity, String> {
    fun findByStreamId(streamId: String): List<StreamRegionEntity>
    fun deleteByStreamId(streamId: String)
}

@Repository
class StreamRepositoryAdapter(private val jpa: StreamEntityRepository, private val clock: HubClock) : StreamRepository {
    override fun findById(id: StreamId): Stream? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findByRoomId(roomId: RoomId): List<Stream> = jpa.findByRoomId(roomId.value).map { it.toDomain() }
    override fun save(stream: Stream): Stream = jpa.save(stream.toEntity()).toDomain()

    private fun StreamEntity.toDomain() = Stream.reconstitute(StreamId(id), RoomId(roomId), streamKey, name, active, version)
    private fun Stream.toEntity() = StreamEntity(id.value, roomId.value, streamKey, name, active, clock.now(), clock.now())
}

@Repository
class StreamRegionRepositoryAdapter(private val jpa: StreamRegionEntityRepository, private val clock: HubClock) : StreamRegionRepository {
    override fun findById(id: StreamId): StreamRegion? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findByStreamId(streamId: StreamId): List<StreamRegion> = jpa.findByStreamId(streamId.value).map { it.toDomain() }
    override fun save(region: StreamRegion): StreamRegion = jpa.save(region.toEntity()).toDomain()
    override fun deleteByStreamId(streamId: StreamId) = jpa.deleteByStreamId(streamId.value)

    private fun StreamRegionEntity.toDomain() = StreamRegion.reconstitute(
        StreamId(id), StreamId(streamId), RegionType.from(regionType), points, label, isStatic, updatedBy, version
    )
    private fun StreamRegion.toEntity() = StreamRegionEntity(
        id.value, streamId.value, regionType.name.lowercase(), points, label, isStatic, updatedBy, clock.now(), clock.now()
    )
}
