package com.hub.surveillance.infrastructure.persistence

import com.hub.surveillance.domain.model.*
import com.hub.surveillance.domain.repository.EpisodeRepository
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.BedId
import com.hub.shared.domain.BaseEntity
import com.hub.shared.time.HubClock
import jakarta.persistence.*
import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
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
    @Enumerated(EnumType.STRING)
    @Column(name = "status") var status: EpisodeStatus = EpisodeStatus.PENDING,
    @Column(name = "status_actor_id") var statusActorId: String? = null,
    @Column(name = "status_at") var statusAt: Instant? = null,
    @Column(name = "title") var title: String? = null,
    @Column(name = "detail") var detail: String? = null,
    @Column(name = "occurred_at") var occurredAt: Instant = Instant.now(),
    @Column(name = "escalation_level") var escalationLevel: Int = 0,
    @Column(name = "escalated_at") var escalatedAt: Instant? = null,
    @Column(name = "escalated_to") var escalatedTo: String? = null,
    @Version var version: Long = 0,
) : BaseEntity()

@Repository
interface EpisodeEntityRepository :
    JpaRepository<EpisodeEntity, String>,
    JpaSpecificationExecutor<EpisodeEntity> {
    fun findByResidentId(residentId: String): List<EpisodeEntity>
    @Query("SELECT e FROM EpisodeEntity e WHERE e.status = 'PENDING'")
    fun findPending(): List<EpisodeEntity>
    @Query("SELECT e FROM EpisodeEntity e WHERE e.residentId = :residentId AND e.status IN ('PENDING','ACKNOWLEDGED') ORDER BY e.occurredAt DESC LIMIT 1")
    fun findOpenByResidentId(residentId: String): EpisodeEntity?
}

@Repository
class EpisodeRepositoryAdapter(private val jpa: EpisodeEntityRepository, private val clock: HubClock) : EpisodeRepository {
    override fun findById(id: EpisodeId): Episode? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findByResidentId(residentId: ResidentId): List<Episode> = jpa.findByResidentId(residentId.value).map { it.toDomain() }
    override fun findPending(): List<Episode> = jpa.findPending().map { it.toDomain() }
    override fun findOpenByResidentId(residentId: ResidentId): Episode? = jpa.findOpenByResidentId(residentId.value)?.toDomain()
    /*
     * Los filtros ausentes no entran a la consulta, en vez de entrar como un
     * parametro nulo.
     *
     * Antes era una @Query con el patron (:param IS NULL OR col = :param). Ese
     * patron se lee bien y no funciona contra PostgreSQL: el SQL que genera
     * Hibernate es `where (? is null or e.status = ?)`, y Postgres no puede
     * inferir el tipo de un parametro que solo se compara con IS NULL. Falla al
     * preparar la sentencia -"could not determine data type of parameter $5"-,
     * asi que fallaba siempre, con o sin filtros, y con un 500 sin mensaje.
     *
     * Con Specification cada filtro presente agrega su predicado y los ausentes
     * no existen: no hay parametro que tipar. Es ademas lo que hace que la
     * consulta pueda usar los indices.
     */
    override fun findFiltered(residentId: ResidentId?, status: String?, from: java.time.Instant?, to: java.time.Instant?): List<Episode> {
        val spec = Specification<EpisodeEntity> { root, _, cb ->
            val predicates = mutableListOf<Predicate>()
            residentId?.let { predicates += cb.equal(root.get<String>("residentId"), it.value) }
            status?.let { predicates += cb.equal(root.get<EpisodeStatus>("status"), EpisodeStatus.from(it)) }
            from?.let { predicates += cb.greaterThanOrEqualTo(root.get("occurredAt"), it) }
            to?.let { predicates += cb.lessThanOrEqualTo(root.get("occurredAt"), it) }
            if (predicates.isEmpty()) cb.conjunction() else cb.and(*predicates.toTypedArray())
        }
        return jpa.findAll(spec).map { it.toDomain() }
    }
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
        severity.name, status, statusActorId, statusAt, title, detail,
        occurredAt, escalationLevel, escalatedAt, escalatedTo
    )
}
