package com.hub.policy.domain.model

import com.hub.shared.domain.AggregateRoot
import java.time.Instant
import java.util.UUID

/**
 * Transactional Outbox para publicar cambios de política a NATS.
 *
 * Fowler: "El outbox no es un dump de datos — es una entity de dominio
 * con un ciclo de vida: nace pendiente, se publica, o falla y reintenta".
 */
class PolicyOutboxEntry private constructor(
    override val id: OutboxEntryId,
    val aggregateId: String,
    val type: String,
    val payloadJson: String,
    val occurredAt: Instant,
    val published: Boolean,
    val attempts: Int,
    val lastError: String?,
    override var version: Long
) : AggregateRoot<OutboxEntryId>() {

    val isPending: Boolean get() = !published

    fun markPublished(): PolicyOutboxEntry {
        require(isPending) { "Entry already published" }
        return reconstitute(
            id = id, aggregateId = aggregateId, type = type, payloadJson = payloadJson,
            occurredAt = occurredAt, published = true, attempts = attempts,
            lastError = null, version = version + 1
        )
    }

    fun recordFailure(error: String): PolicyOutboxEntry {
        require(isPending) { "Cannot record failure on published entry" }
        return reconstitute(
            id = id, aggregateId = aggregateId, type = type, payloadJson = payloadJson,
            occurredAt = occurredAt, published = false, attempts = attempts + 1,
            lastError = error, version = version + 1
        )
    }

    companion object {
        fun create(aggregateId: String, type: String, payloadJson: String): PolicyOutboxEntry =
            PolicyOutboxEntry(
                id = OutboxEntryId.random(), aggregateId = aggregateId, type = type,
                payloadJson = payloadJson, occurredAt = Instant.now(),
                published = false, attempts = 0, lastError = null, version = 0
            )

        fun reconstitute(
            id: OutboxEntryId, aggregateId: String, type: String, payloadJson: String,
            occurredAt: Instant, published: Boolean, attempts: Int,
            lastError: String?, version: Long
        ): PolicyOutboxEntry = PolicyOutboxEntry(
            id, aggregateId, type, payloadJson, occurredAt, published, attempts, lastError, version
        )
    }
}

@JvmInline
value class OutboxEntryId(val value: String) {
    companion object {
        fun from(value: String): OutboxEntryId = OutboxEntryId(value)
        fun random(): OutboxEntryId = OutboxEntryId(UUID.randomUUID().toString())
    }
}
