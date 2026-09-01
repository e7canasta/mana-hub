package com.hub.policy.infrastructure.persistence

import com.hub.policy.domain.model.OutboxEntryId
import com.hub.policy.domain.model.PolicyOutboxEntry
import com.hub.shared.domain.BaseEntity
import jakarta.persistence.*
import org.hibernate.annotations.Immutable
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
    @Column(name = "last_error") var lastError: String? = null,
) : BaseEntity()

@Repository
interface PolicyOutboxEntityRepository : JpaRepository<PolicyOutboxEntity, String> {
    @Query("SELECT e FROM PolicyOutboxEntity e WHERE e.published = false ORDER BY e.occurredAt ASC")
    fun findUnpublished(): List<PolicyOutboxEntity>
}

@Repository
class PolicyOutboxRepositoryAdapter(
    private val jpa: PolicyOutboxEntityRepository
) : PolicyOutboxRepository {

    override fun findUnpublished(): List<PolicyOutboxEntry> =
        jpa.findUnpublished().map { it.toDomain() }

    override fun save(entry: PolicyOutboxEntry): PolicyOutboxEntry =
        jpa.save(entry.toEntity()).toDomain()

    override fun saveAll(entries: List<PolicyOutboxEntry>): List<PolicyOutboxEntry> =
        jpa.saveAll(entries.map { it.toEntity() }).map { it.toDomain() }

    private fun PolicyOutboxEntity.toDomain() = PolicyOutboxEntry.reconstitute(
        id = OutboxEntryId(id), aggregateId = aggregateId, type = type,
        payloadJson = payloadJson, occurredAt = occurredAt, published = published,
        attempts = attempts, lastError = lastError, version = 0L
    )

    private fun PolicyOutboxEntry.toEntity() = PolicyOutboxEntity(
        id = id.value, aggregateId = aggregateId, type = type,
        payloadJson = payloadJson, occurredAt = occurredAt, published = published,
        attempts = attempts, lastError = lastError
    )
}

interface PolicyOutboxRepository {
    fun findUnpublished(): List<PolicyOutboxEntry>
    fun save(entry: PolicyOutboxEntry): PolicyOutboxEntry
    fun saveAll(entries: List<PolicyOutboxEntry>): List<PolicyOutboxEntry>
}
