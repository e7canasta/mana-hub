package com.hub.coverage.infrastructure.persistence

import com.hub.coverage.domain.model.*
import com.hub.coverage.domain.repository.ShiftRepository
import com.hub.coverage.domain.repository.StaffGroupRepository
import com.hub.coverage.domain.repository.StaffMemberRepository
import com.hub.shared.domain.FacilityId
import com.hub.shared.domain.StaffMemberId
import com.hub.shared.domain.UserId
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Entity
@Table(name = "staff_groups")
class StaffGroupEntity(
    @Id var id: String = "",
    @Column(name = "facility_id") var facilityId: String = "",
    @Column(name = "name") var name: String = "",
    @Column(name = "retired_at") var retiredAt: Instant? = null,
    @Column(name = "retired_by") var retiredBy: String? = null,
    @Column(name = "created_at") var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at") var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0
)

@Entity
@Table(name = "facility_shifts")
class FacilityShiftEntity(
    @Id var id: String = "",
    @Column(name = "facility_id") var facilityId: String = "",
    @Column(name = "key") var key: String = "",
    @Column(name = "label") var label: String = "",
    @Column(name = "start_minute") var startMinute: Int = 0,
    @Column(name = "sort_order") var sortOrder: Int = 0,
    @Column(name = "retired_at") var retiredAt: Instant? = null,
    @Column(name = "retired_by") var retiredBy: String? = null,
    @Column(name = "created_at") var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at") var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0
)

@Entity
@Table(name = "unit_shift_coverages")
class UnitShiftCoverageEntity(
    @Id var id: String = "",
    @Column(name = "wing_id") var wingId: String = "",
    @Column(name = "staff_group_id") var staffGroupId: String = "",
    @Column(name = "shift_key") var shiftKey: String = "",
    @Column(name = "valid_from") var validFrom: Instant = Instant.now(),
    @Column(name = "valid_to") var validTo: Instant? = null,
    @Column(name = "created_at") var createdAt: Instant = Instant.now(),
    @Column(name = "created_by") var createdBy: String? = null
)

@Entity
@Table(name = "staff_members")
class StaffMemberEntity(
    @Id var id: String = "",
    @Column(name = "facility_id") var facilityId: String = "",
    @Column(name = "full_name") var fullName: String = "",
    @Column(name = "role") var role: String = "",
    @Column(name = "user_id") var userId: String? = null,
    @Column(name = "retired_at") var retiredAt: Instant? = null,
    @Column(name = "retired_by") var retiredBy: String? = null,
    @Column(name = "created_at") var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at") var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0
)

@Repository
interface StaffGroupEntityRepository : JpaRepository<StaffGroupEntity, String> {
    fun findByFacilityId(facilityId: String): List<StaffGroupEntity>
}

@Repository
interface FacilityShiftEntityRepository : JpaRepository<FacilityShiftEntity, String> {
    fun findByFacilityId(facilityId: String): List<FacilityShiftEntity>
}

@Repository
interface UnitShiftCoverageEntityRepository : JpaRepository<UnitShiftCoverageEntity, String> {
    fun findByWingId(wingId: String): List<UnitShiftCoverageEntity>
}

@Repository
interface StaffMemberEntityRepository : JpaRepository<StaffMemberEntity, String> {
    fun findByFacilityId(facilityId: String): List<StaffMemberEntity>
    fun findByUserId(userId: String): StaffMemberEntity?
}

// ── Adapters: Domain Repository → JPA ──────────────────────────

@Repository
class StaffGroupRepositoryAdapter(private val jpa: StaffGroupEntityRepository) : StaffGroupRepository {
    override fun findById(id: StaffGroupId): StaffGroup? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findByFacilityId(facilityId: FacilityId): List<StaffGroup> = jpa.findByFacilityId(facilityId.value).map { it.toDomain() }
    override fun save(staffGroup: StaffGroup): StaffGroup = jpa.save(staffGroup.toEntity()).toDomain()

    private fun StaffGroupEntity.toDomain() = StaffGroup.reconstitute(
        StaffGroupId(id), FacilityId(facilityId), name,
        retiredAt, retiredBy, version
    )
    private fun StaffGroup.toEntity() = StaffGroupEntity(
        id.value, facilityId.value, name,
        retiredAt, retiredBy, Instant.now(), Instant.now(), version
    )
}

@Repository
class ShiftRepositoryAdapter(private val jpa: FacilityShiftEntityRepository) : ShiftRepository {
    override fun findById(id: StaffGroupId): FacilityShift? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findByFacilityId(facilityId: FacilityId): List<FacilityShift> = jpa.findByFacilityId(facilityId.value).map { it.toDomain() }
    override fun save(shift: FacilityShift): FacilityShift = jpa.save(shift.toEntity()).toDomain()

    private fun FacilityShiftEntity.toDomain() = FacilityShift.reconstitute(
        StaffGroupId(id), FacilityId(facilityId), key, label, startMinute, sortOrder,
        retiredAt, retiredBy, version
    )
    private fun FacilityShift.toEntity() = FacilityShiftEntity(
        id.value, facilityId.value, key, label, startMinute, sortOrder,
        retiredAt, retiredBy, Instant.now(), Instant.now(), version
    )
}

@Repository
class StaffMemberRepositoryAdapter(private val jpa: StaffMemberEntityRepository) : StaffMemberRepository {
    override fun findById(id: StaffMemberId): StaffMember? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findByFacilityId(facilityId: String): List<StaffMember> = jpa.findByFacilityId(facilityId).map { it.toDomain() }
    override fun findByUserId(userId: String): StaffMember? = jpa.findByUserId(userId)?.toDomain()
    override fun save(member: StaffMember): StaffMember = jpa.save(member.toEntity()).toDomain()

    private fun StaffMemberEntity.toDomain() = StaffMember.reconstitute(
        StaffMemberId(id), FacilityId(facilityId), fullName, StaffRole.valueOf(role),
        userId?.let { UserId(it) }, version
    )
    private fun StaffMember.toEntity() = StaffMemberEntity(
        id.value, facilityId.value, fullName, role.name, userId?.value,
        null, null, Instant.now(), Instant.now(), version
    )
}
