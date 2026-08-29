package com.hub.bridge

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * Transactional Outbox — hub → hive
 * Cada PATCH /api/v1/alarm-presets/{id} escribe aquí en la misma TX que alarm_profile_versions.
 * Un relay publica a NATS JetStream y marca published=true.
 * Patrón Fowler: "Transactional Outbox" — evita dual-write (DB + bus sin TX).
 */
@Entity
@Table(name = "hub_policy_outbox")
class HubPolicyOutboxEntity(
    @Id var id: String = "",
    @Column(name = "aggregate_id") var aggregateId: String = "",
    @Column(name = "type") var type: String = "",
    @Column(name = "payload_json", columnDefinition = "TEXT") var payloadJson: String = "",
    @Column(name = "occurred_at") var occurredAt: Instant = Instant.now(),
    @Column(name = "published") var published: Boolean = false,
    @Column(name = "attempts") var attempts: Int = 0,
    @Column(name = "last_error") var lastError: String? = null
)

@Repository
interface HubPolicyOutboxRepository : JpaRepository<HubPolicyOutboxEntity, String> {
    @Query("SELECT e FROM HubPolicyOutboxEntity e WHERE e.published = false ORDER BY e.occurredAt ASC LIMIT 100")
    fun findUnpublished(): List<HubPolicyOutboxEntity>
}
