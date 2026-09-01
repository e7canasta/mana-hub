package com.hub.policy.infrastructure.persistence

import com.hub.policy.domain.model.*
import com.hub.policy.domain.repository.AlarmProfileRepository
import com.hub.shared.domain.BaseEntity
import com.hub.shared.domain.ResidentId
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Entity
@Table(name = "alarm_profile_versions")
class AlarmProfileVersionEntity(
    @Id var id: String = "",
    @Column(name = "resident_id") var residentId: String = "",
    @Column(name = "valid_from") var validFrom: Instant = Instant.now(),
    @Column(name = "valid_to") var validTo: Instant? = null,
    @Column(name = "mobility_aid") var mobilityAid: String? = null,
    @Column(name = "autopilot") var autopilot: Boolean = false,
    @Column(name = "mode") var mode: String? = null,
    @Column(name = "template_id") var templateId: String? = null,
    @Column(name = "catalog_version") var catalogVersion: String? = null,
    @Column(name = "updated_by") var updatedBy: String? = null,
    @Column(name = "risk_level") var riskLevel: String = "medium",
    @Version var version: Long = 0,
) : BaseEntity()

@Repository
interface AlarmProfileVersionEntityRepository : JpaRepository<AlarmProfileVersionEntity, String> {
    @Query("SELECT e FROM AlarmProfileVersionEntity e WHERE e.residentId = :residentId AND e.validTo IS NULL")
    fun findCurrentByResidentId(residentId: String): AlarmProfileVersionEntity?
    fun findByResidentId(residentId: String): List<AlarmProfileVersionEntity>
}

@Repository
class AlarmProfileRepositoryAdapter(private val jpa: AlarmProfileVersionEntityRepository) : AlarmProfileRepository {
    override fun findById(id: AlarmProfileId): AlarmProfileVersion? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findCurrentByResidentId(residentId: ResidentId): AlarmProfileVersion? = jpa.findCurrentByResidentId(residentId.value)?.toDomain()
    override fun findByResidentId(residentId: ResidentId): List<AlarmProfileVersion> = jpa.findByResidentId(residentId.value).map { it.toDomain() }
    override fun save(profile: AlarmProfileVersion): AlarmProfileVersion = jpa.save(profile.toEntity()).toDomain()
    override fun expireCurrentByResidentId(residentId: ResidentId) {
        val current = jpa.findCurrentByResidentId(residentId.value) ?: return
        current.validTo = java.time.Instant.now()
        jpa.saveAndFlush(current)
    }

    private fun AlarmProfileVersionEntity.toDomain() = AlarmProfileVersion.reconstitute(
        id = AlarmProfileId(id), residentId = ResidentId(residentId), validFrom = validFrom, validTo = validTo,
        mobilityAid = mobilityAid?.let { MobilityAid.from(it) },
        autopilot = autopilot,
        mode = mode?.let { PolicyMode.from(it) },
        templateId = templateId?.let { TemplateId.from(it) },
        catalogVersion = catalogVersion, updatedBy = updatedBy,
        riskLevel = RiskLevel.from(riskLevel), version = version
    )
    private fun AlarmProfileVersion.toEntity() = AlarmProfileVersionEntity(
        id.value, residentId.value, validFrom, validTo,
        mobilityAid?.name?.lowercase(), autopilot,
        mode?.name?.lowercase(),
        templateId?.value,
        catalogVersion, updatedBy, riskLevel.name.lowercase()
    )
}
