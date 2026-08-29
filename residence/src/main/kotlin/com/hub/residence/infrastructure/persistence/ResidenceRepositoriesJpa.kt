package com.hub.residence.infrastructure.persistence

import com.hub.residence.domain.model.*
import com.hub.shared.domain.BedId
import com.hub.shared.domain.FacilityId
import com.hub.residence.domain.repository.*
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Entity
@Table(name = "facilities")
class FacilityEntity(
    @Id var id: String = "",
    @Column(name = "name") var name: String = "",
    @Column(name = "timezone") var timezone: String = "UTC",
    @Column(name = "retired_at") var retiredAt: Instant? = null,
    @Column(name = "retired_by") var retiredBy: String? = null,
    @Column(name = "created_at") var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at") var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0
)

@Entity
@Table(name = "wings")
class WingEntity(
    @Id var id: String = "",
    @Column(name = "facility_id") var facilityId: String = "",
    @Column(name = "name") var name: String = "",
    @Column(name = "floor") var floor: String? = null,
    @Column(name = "sort_order") var sortOrder: Int = 0,
    @Column(name = "retired_at") var retiredAt: Instant? = null,
    @Column(name = "retired_by") var retiredBy: String? = null,
    @Column(name = "created_at") var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at") var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0
)

@Entity
@Table(name = "rooms")
class RoomEntity(
    @Id var id: String = "",
    @Column(name = "wing_id") var wingId: String = "",
    @Column(name = "number") var number: String = "",
    @Column(name = "room_type") var roomType: String? = null,
    @Column(name = "stream_key") var streamKey: String? = null,
    @Column(name = "retired_at") var retiredAt: Instant? = null,
    @Column(name = "retired_by") var retiredBy: String? = null,
    @Column(name = "created_at") var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at") var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0
)

@Entity
@Table(name = "beds")
class BedEntity(
    @Id var id: String = "",
    @Column(name = "room_id") var roomId: String = "",
    @Column(name = "label") var label: String = "",
    @Column(name = "monitor_key") var monitorKey: String? = null,
    @Column(name = "retired_at") var retiredAt: Instant? = null,
    @Column(name = "retired_by") var retiredBy: String? = null,
    @Column(name = "created_at") var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at") var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0
)

@Entity
@Table(name = "planogram_placements")
class PlanogramPlacementEntity(
    @Id var id: String = "",
    @Column(name = "wing_id") var wingId: String = "",
    @Column(name = "room_id") var roomId: String = "",
    var x: Double = 0.0,
    var y: Double = 0.0,
    @Column(name = "sort_order") var sortOrder: Int = 0,
    var active: Boolean = true
)

@Entity
@Table(name = "room_privacy_regions")
class RoomPrivacyRegionEntity(
    @Id var id: String = "",
    @Column(name = "room_id") var roomId: String = "",
    var x: Double = 0.0,
    var y: Double = 0.0,
    var w: Double = 0.0,
    var h: Double = 0.0,
    var active: Boolean = true
)

@Repository
interface FacilityEntityRepository : JpaRepository<FacilityEntity, String>

@Repository
interface WingEntityRepository : JpaRepository<WingEntity, String> {
    fun findByFacilityId(facilityId: String): List<WingEntity>
}

@Repository
interface RoomEntityRepository : JpaRepository<RoomEntity, String> {
    fun findByWingId(wingId: String): List<RoomEntity>
}

@Repository
interface BedEntityRepository : JpaRepository<BedEntity, String> {
    fun findByRoomId(roomId: String): List<BedEntity>
}

@Repository
interface PlanogramPlacementEntityRepository : JpaRepository<PlanogramPlacementEntity, String> {
    fun findByWingIdAndActiveTrueOrderBySortOrderAsc(wingId: String): List<PlanogramPlacementEntity>
    fun deleteByWingId(wingId: String)
}

@Repository
interface RoomPrivacyRegionEntityRepository : JpaRepository<RoomPrivacyRegionEntity, String> {
    fun findByRoomIdAndActiveTrue(roomId: String): List<RoomPrivacyRegionEntity>
    fun deleteByRoomId(roomId: String)
}

@Repository
class FacilityRepositoryAdapter(private val jpa: FacilityEntityRepository) : FacilityRepository {
    override fun findById(id: FacilityId): Facility? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findAll(): List<Facility> = jpa.findAll().map { it.toDomain() }
    override fun save(facility: Facility): Facility = jpa.save(facility.toEntity()).toDomain()

    private fun FacilityEntity.toDomain() = Facility.reconstitute(FacilityId(id), name, timezone, retiredAt, retiredBy, version)
    private fun Facility.toEntity() = FacilityEntity(id.value, name, timezone, retiredAt, retiredBy, Instant.now(), Instant.now(), version)
}

@Repository
class WingRepositoryAdapter(private val jpa: WingEntityRepository) : WingRepository {
    override fun findById(id: WingId): Wing? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findByFacilityId(facilityId: FacilityId): List<Wing> = jpa.findByFacilityId(facilityId.value).map { it.toDomain() }
    override fun save(wing: Wing): Wing = jpa.save(wing.toEntity()).toDomain()

    private fun WingEntity.toDomain() = Wing.reconstitute(WingId(id), FacilityId(facilityId), name, floor, sortOrder, retiredAt, retiredBy, version)
    private fun Wing.toEntity() = WingEntity(id.value, facilityId.value, name, floor, sortOrder, retiredAt, retiredBy, Instant.now(), Instant.now(), version)
}

@Repository
class RoomRepositoryAdapter(private val jpa: RoomEntityRepository) : RoomRepository {
    override fun findById(id: RoomId): Room? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findByWingId(wingId: WingId): List<Room> = jpa.findByWingId(wingId.value).map { it.toDomain() }
    override fun save(room: Room): Room = jpa.save(room.toEntity()).toDomain()

    private fun RoomEntity.toDomain() = Room.reconstitute(RoomId(id), WingId(wingId), number, roomType, streamKey, retiredAt, retiredBy, version)
    private fun Room.toEntity() = RoomEntity(id.value, wingId.value, number, roomType, streamKey, retiredAt, retiredBy, Instant.now(), Instant.now(), version)
}

@Repository
class BedRepositoryAdapter(private val jpa: BedEntityRepository) : BedRepository {
    override fun findById(id: BedId): Bed? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findByRoomId(roomId: RoomId): List<Bed> = jpa.findByRoomId(roomId.value).map { it.toDomain() }
    override fun save(bed: Bed): Bed = jpa.save(bed.toEntity()).toDomain()
    override fun saveAndFlush(bed: Bed): Bed = jpa.saveAndFlush(bed.toEntity()).toDomain()

    override fun findAll(): List<Bed> = jpa.findAll().map { it.toDomain() }

    private fun BedEntity.toDomain() = Bed.reconstitute(BedId(id), RoomId(roomId), label, monitorKey, retiredAt, retiredBy, version)
    private fun Bed.toEntity() = BedEntity(id.value, roomId.value, label, monitorKey, retiredAt, retiredBy, Instant.now(), Instant.now(), version)
}

@Repository
class PlanogramRepositoryAdapter(private val jpa: PlanogramPlacementEntityRepository) : PlanogramRepository {
    override fun findActiveByWingId(wingId: WingId): List<PlanogramPlacement> =
        jpa.findByWingIdAndActiveTrueOrderBySortOrderAsc(wingId.value).map { it.toDomain() }
    override fun deleteByWingId(wingId: WingId) = jpa.deleteByWingId(wingId.value)
    override fun saveAll(placements: List<PlanogramPlacement>): List<PlanogramPlacement> =
        jpa.saveAll(placements.map { it.toEntity() }).map { it.toDomain() }

    private fun PlanogramPlacementEntity.toDomain() =
        PlanogramPlacement(id, WingId(wingId), RoomId(roomId), x, y, sortOrder, active)
    private fun PlanogramPlacement.toEntity() =
        PlanogramPlacementEntity(id, wingId.value, roomId.value, x, y, sortOrder, active)
}

@Repository
class RoomPrivacyRegionRepositoryAdapter(private val jpa: RoomPrivacyRegionEntityRepository) : RoomPrivacyRegionRepository {
    override fun findActiveByRoomId(roomId: RoomId): List<RoomPrivacyRegion> =
        jpa.findByRoomIdAndActiveTrue(roomId.value).map { it.toDomain() }
    override fun deleteByRoomId(roomId: RoomId) = jpa.deleteByRoomId(roomId.value)
    override fun saveAll(regions: List<RoomPrivacyRegion>): List<RoomPrivacyRegion> =
        jpa.saveAll(regions.map { it.toEntity() }).map { it.toDomain() }

    private fun RoomPrivacyRegionEntity.toDomain() =
        RoomPrivacyRegion(id, RoomId(roomId), x, y, w, h, active)
    private fun RoomPrivacyRegion.toEntity() =
        RoomPrivacyRegionEntity(id, roomId.value, x, y, w, h, active)
}
