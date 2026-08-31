package com.hub.policy.infrastructure.persistence

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Entity
@Table(name = "hub_policy_outbox")
class PolicyOutboxEntity(
    @Id var id: String = "",
    @Column(name = "aggregate_id") var aggregateId: String = "",
    @Column(name = "type") var type: String = "",
    @Column(name = "payload_json") var payloadJson: String = "",
    @Column(name = "occurred_at") var occurredAt: Instant = Instant.now(),
    @Column(name = "published") var published: Boolean = false,
    @Column(name = "attempts") var attempts: Int = 0,
    @Column(name = "last_error") var lastError: String? = null
)

@Repository
interface PolicyOutboxEntityRepository : JpaRepository<PolicyOutboxEntity, String> {
    @Query("SELECT e FROM PolicyOutboxEntity e WHERE e.published = false ORDER BY e.occurredAt ASC")
    fun findUnpublished(): List<PolicyOutboxEntity>
}
