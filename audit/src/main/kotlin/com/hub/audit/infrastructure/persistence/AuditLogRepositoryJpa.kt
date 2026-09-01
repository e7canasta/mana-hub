package com.hub.audit.infrastructure.persistence

import com.hub.audit.domain.model.AuditLogEntry
import com.hub.audit.domain.model.AuditLogId
import com.hub.audit.domain.repository.AuditLogRepository
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Entity
@Table(name = "audit_log")
class AuditLogEntity(
    @Id
    @Column(name = "id")
    var id: String = "",

    @Column(name = "actor_id")
    var actorId: String? = null,

    @Column(name = "action")
    var action: String = "",

    @Column(name = "entity_type")
    var entityType: String = "",

    @Column(name = "entity_id")
    var entityId: String = "",

    @Column(name = "metadata_json")
    var metadataJson: String = "{}",

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now(),
)

@Repository
interface AuditLogEntityRepository : JpaRepository<AuditLogEntity, String> {
    fun findByEntityTypeAndEntityId(entityType: String, entityId: String): List<AuditLogEntity>
    fun findByActorId(actorId: String): List<AuditLogEntity>
}

@Repository
class AuditLogRepositoryAdapter(
    private val jpa: AuditLogEntityRepository
) : AuditLogRepository {

    override fun save(entry: AuditLogEntry): AuditLogEntry {
        val entity = AuditLogEntity(
            id = entry.id.value,
            actorId = entry.actorId,
            action = entry.action,
            entityType = entry.entityType,
            entityId = entry.entityId,
            metadataJson = entry.metadataJson,
        )
        entity.createdAt = entry.createdAt
        jpa.save(entity)
        return entry
    }

    override fun findById(id: AuditLogId): AuditLogEntry? {
        return jpa.findById(id.value).orElse(null)?.toDomain()
    }

    override fun findByEntityTypeAndEntityId(entityType: String, entityId: String): List<AuditLogEntry> {
        return jpa.findByEntityTypeAndEntityId(entityType, entityId).map { it.toDomain() }
    }

    override fun findByActorId(actorId: String): List<AuditLogEntry> {
        return jpa.findByActorId(actorId).map { it.toDomain() }
    }

    private fun AuditLogEntity.toDomain(): AuditLogEntry = AuditLogEntry(
        id = AuditLogId(id),
        actorId = actorId,
        action = action,
        entityType = entityType,
        entityId = entityId,
        metadataJson = metadataJson,
        createdAt = createdAt!!
    )
}
