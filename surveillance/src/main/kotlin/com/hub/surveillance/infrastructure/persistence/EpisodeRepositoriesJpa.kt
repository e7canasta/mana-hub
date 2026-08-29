package com.hub.surveillance.infrastructure.persistence

import com.hub.surveillance.domain.model.*
import com.hub.surveillance.domain.repository.EpisodeRepository
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.BedId
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Entity
@Table(name = "episodes")
class EpisodeEntity(
    @Id var id: String = "",
    @Column(name = "resident_id") var residentId: String = "",
    @Column(name = "bed_id") var bedId: String? = null,
    @Column(name = "evidence_kind") var evidenceKind: String? = null,
    @Column(name = "evidence_ref") var evidenceRef: String? = null,
    @Column(name = "rule_id") var ruleId: String? = null,
    @Column(name = "severity") var severity: String = "",
    @Column(name = "status") var status: String = "pending",
    @Column(name = "status_actor_id") var statusActorId: String? = null,
    @Column(name = "status_at") var statusAt: Instant? = null,
    @Column(name = "title") var title: String? = null,
    @Column(name = "detail") var detail: String? = null,
    @Column(name = "occurred_at") var occurredAt: Instant = Instant.now(),
    @Column(name = "escalation_level") var escalationLevel: Int = 0,
    @Column(name = "escalated_at") var escalatedAt: Instant? = null,
    @Column(name = "escalated_to") var escalatedTo: String? = null,
    @Column(name = "created_at") var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at") var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0
)

@Repository
interface EpisodeEntityRepository : JpaRepository<EpisodeEntity, String> {
    fun findByResidentId(residentId: String): List<EpisodeEntity>
    @Query("SELECT e FROM EpisodeEntity e WHERE e.status = 'pending'")
    fun findPending(): List<EpisodeEntity>
    @Query("SELECT e FROM EpisodeEntity e WHERE e.residentId = :residentId AND e.status IN ('pending','acknowledged') ORDER BY e.occurredAt DESC LIMIT 1")
    fun findOpenByResidentId(residentId: String): EpisodeEntity?
    @Query("""
        SELECT e FROM EpisodeEntity e
        WHERE (:residentId IS NULL OR e.residentId = :residentId)
          AND (:status IS NULL OR e.status = :status)
          AND (:fromDate IS NULL OR e.occurredAt >= :fromDate)
          AND (:toDate IS NULL OR e.occurredAt <= :toDate)
    """)
    fun findFiltered(
        residentId: String?,
        status: String?,
        fromDate: Instant?,
        toDate: Instant?
    ): List<EpisodeEntity>
}

@Repository
class EpisodeRepositoryAdapter(private val jpa: EpisodeEntityRepository) : EpisodeRepository {
    override fun findById(id: EpisodeId): Episode? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findByResidentId(residentId: ResidentId): List<Episode> = jpa.findByResidentId(residentId.value).map { it.toDomain() }
    override fun findPending(): List<Episode> = jpa.findPending().map { it.toDomain() }
    override fun findOpenByResidentId(residentId: ResidentId): Episode? = jpa.findOpenByResidentId(residentId.value)?.toDomain()
    override fun findFiltered(residentId: ResidentId?, status: String?, from: java.time.Instant?, to: java.time.Instant?): List<Episode> =
        jpa.findFiltered(residentId?.value, status, from, to).map { it.toDomain() }
    override fun save(episode: Episode): Episode {
        val existing = jpa.findById(episode.id.value).orElse(null)
        val entity = episode.toEntity()
        if (existing != null) {
            entity.version = existing.version
        }
        return jpa.save(entity).toDomain()
    }

    private fun EpisodeEntity.toDomain() = Episode.reconstitute(
        EpisodeId(id), ResidentId(residentId), bedId?.let { BedId(it) }, evidenceKind,
        evidenceRef, ruleId, EpisodeSeverity.from(severity), status, statusActorId, statusAt,
        title, detail, occurredAt, escalationLevel, escalatedAt, escalatedTo, version
    )
    private fun Episode.toEntity() = EpisodeEntity(
        id.value, residentId.value, bedId?.value, evidenceKind, evidenceRef, ruleId,
        severity.name.lowercase(), status, statusActorId, statusAt, title, detail,
        occurredAt, escalationLevel, escalatedAt, escalatedTo, Instant.now(), Instant.now()
    )
}
