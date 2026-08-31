package com.hub.coverage.infrastructure.persistence

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
    @Column(name = "retired_at") var retiredAt: String? = null,
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
    @Column(name = "retired_at") var retiredAt: String? = null,
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
    @Column(name = "retired_at") var retiredAt: String? = null,
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
