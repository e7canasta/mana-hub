package com.hub.population.infrastructure.persistence

import com.hub.population.domain.model.*
import com.hub.population.domain.repository.BedAssignmentRepository
import com.hub.population.domain.repository.ResidentRepository
import com.hub.shared.domain.BedId
import com.hub.shared.domain.ResidentId
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "residents")
class ResidentEntity(
    @Id var id: String = "",
    @Column(name = "external_id") var externalId: String? = null,
    @Column(name = "full_name") var fullName: String = "",
    @Column(name = "birth_date") var birthDate: LocalDate? = null,
    @Column(name = "admission_date") var admissionDate: LocalDate = LocalDate.now(),
    @Column(name = "status") var status: String = "active",
    @Column(name = "discharged_at") var dischargedAt: Instant? = null,
    @Column(name = "discharged_by") var dischargedBy: String? = null,
    @Column(name = "created_at") var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at") var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0
)

@Entity
@Table(name = "resident_bed_assignments")
class BedAssignmentEntity(
    @Id var id: String = "",
    @Column(name = "resident_id") var residentId: String = "",
    @Column(name = "bed_id") var bedId: String = "",
    @Column(name = "starts_at") var startsAt: Instant = Instant.now(),
    @Column(name = "ends_at") var endsAt: Instant? = null,
    @Column(name = "created_by") var createdBy: String? = null,
    @Column(name = "created_at") var createdAt: Instant = Instant.now(),
    @Version var version: Long = 0
)

@Repository
interface ResidentEntityRepository : JpaRepository<ResidentEntity, String> {
    fun findByExternalId(externalId: String): ResidentEntity?
    fun existsByExternalId(externalId: String): Boolean
}

@Repository
interface BedAssignmentEntityRepository : JpaRepository<BedAssignmentEntity, String> {
    fun findByResidentId(residentId: String): List<BedAssignmentEntity>
    fun findByBedId(bedId: String): BedAssignmentEntity?
    fun findByEndsAtIsNull(): List<BedAssignmentEntity>
    @Query("SELECT e FROM BedAssignmentEntity e WHERE e.residentId = :residentId AND e.endsAt IS NULL")
    fun findOpenByResidentId(residentId: String): BedAssignmentEntity?
    @Query("SELECT e FROM BedAssignmentEntity e WHERE e.bedId = :bedId AND e.endsAt IS NULL")
    fun findOpenByBedId(bedId: String): BedAssignmentEntity?
}

@Repository
class ResidentRepositoryAdapter(private val jpa: ResidentEntityRepository) : ResidentRepository {
    override fun findById(id: ResidentId): Resident? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findByExternalId(externalId: String): Resident? = jpa.findByExternalId(externalId)?.toDomain()
    override fun findAll(): List<Resident> = jpa.findAll().map { it.toDomain() }
    override fun save(resident: Resident): Resident = jpa.save(resident.toEntity()).toDomain()
    override fun existsByExternalId(externalId: String): Boolean = jpa.existsByExternalId(externalId)

    private fun ResidentEntity.toDomain() = Resident.reconstitute(
        ResidentId(id), externalId, fullName, birthDate, admissionDate,
        ResidentStatus.from(status), dischargedAt, dischargedBy, version
    )
    private fun Resident.toEntity() = ResidentEntity(
        id.value, externalId, fullName, birthDate, admissionDate, status.name.lowercase(),
        dischargedAt, dischargedBy, Instant.now(), Instant.now()
    )
}

@Repository
class BedAssignmentRepositoryAdapter(private val jpa: BedAssignmentEntityRepository) : BedAssignmentRepository {
    override fun findById(id: AssignmentId): BedAssignment? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findByResidentId(residentId: ResidentId): List<BedAssignment> = jpa.findByResidentId(residentId.value).map { it.toDomain() }
    override fun findAllOpen(): List<BedAssignment> = jpa.findByEndsAtIsNull().map { it.toDomain() }
    override fun findByBedId(bedId: BedId): BedAssignment? = jpa.findByBedId(bedId.value)?.toDomain()
    override fun findOpenByResidentId(residentId: ResidentId): BedAssignment? = jpa.findOpenByResidentId(residentId.value)?.toDomain()
    override fun findOpenByBedId(bedId: BedId): BedAssignment? = jpa.findOpenByBedId(bedId.value)?.toDomain()
    override fun save(assignment: BedAssignment): BedAssignment = jpa.save(assignment.toEntity()).toDomain()
    override fun closeAssignment(assignment: BedAssignment) {
        val entity = jpa.findById(assignment.id.value).orElse(null) ?: return
        entity.endsAt = java.time.Instant.now()
        jpa.saveAndFlush(entity)
    }

    private fun BedAssignmentEntity.toDomain() = BedAssignment.reconstitute(
        AssignmentId(id), ResidentId(residentId), BedId(bedId), startsAt, endsAt, createdBy, version
    )
    private fun BedAssignment.toEntity() = BedAssignmentEntity(
        id.value, residentId.value, bedId.value, startsAt, endsAt, createdBy, Instant.now()
    )
}
