package com.hub.integration.infrastructure.persistence

import com.hub.integration.domain.model.ResidentProfile
import com.hub.integration.domain.repository.ResidentProfileRepository
import com.hub.shared.domain.BaseEntity
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Component
import java.time.Instant

@Entity
@Table(name = "resident_profiles")
class ResidentProfileEntity(
    @Id val id: String,
    val residentId: String,
    val profileId: String,
    @Column(name = "version") val profileVersion: Int,
    val supersedes: Int?,
    val validFrom: Instant,
    @Column(columnDefinition = "TEXT") val provenanceJson: String,
    @Column(columnDefinition = "TEXT") val windowsJson: String,
    @Column(columnDefinition = "TEXT") val subjectsJson: String,
    @Column(columnDefinition = "TEXT") val rawJson: String,
) : BaseEntity()

interface ResidentProfileEntityRepository : JpaRepository<ResidentProfileEntity, String> {
    fun findTopByResidentIdOrderByVersionDesc(residentId: String): ResidentProfileEntity?

    fun findByResidentId(residentId: String): List<ResidentProfileEntity>

    @Query("SELECT e FROM ResidentProfileEntity e WHERE e.profileVersion = (SELECT MAX(e2.profileVersion) FROM ResidentProfileEntity e2 WHERE e2.residentId = e.residentId)")
    fun findActiveProfiles(): List<ResidentProfileEntity>
}

@Component
class ResidentProfileRepositoryAdapter(
    private val jpa: ResidentProfileEntityRepository,
) : ResidentProfileRepository {

    override fun findCurrentByResidentId(residentId: String): ResidentProfile? =
        jpa.findTopByResidentIdOrderByVersionDesc(residentId)?.toDomain()

    override fun findByResidentId(residentId: String): List<ResidentProfile> =
        jpa.findByResidentId(residentId).map { it.toDomain() }

    override fun save(profile: ResidentProfile): ResidentProfile =
        jpa.save(profile.toEntity()).toDomain()

    override fun saveAndFlush(profile: ResidentProfile): ResidentProfile =
        jpa.saveAndFlush(profile.toEntity()).toDomain()

    override fun findActiveProfiles(): List<ResidentProfile> =
        jpa.findActiveProfiles().map { it.toDomain() }

    private fun ResidentProfileEntity.toDomain() = ResidentProfile(
        id = id, residentId = residentId, profileId = profileId,
        version = profileVersion, supersedes = supersedes, validFrom = validFrom,
        provenanceJson = provenanceJson, windowsJson = windowsJson,
        subjectsJson = subjectsJson, rawJson = rawJson, createdAt = createdAt!!,
    )

    private fun ResidentProfile.toEntity(): ResidentProfileEntity {
        val entity = ResidentProfileEntity(
            id = id, residentId = residentId, profileId = profileId,
            profileVersion = version, supersedes = supersedes, validFrom = validFrom,
            provenanceJson = provenanceJson, windowsJson = windowsJson,
            subjectsJson = subjectsJson, rawJson = rawJson,
        )
        entity.createdAt = createdAt
        return entity
    }
}
