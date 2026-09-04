/**
 * SOURCE OF TRUTH KEYWORDS: NatsDomainEventPublisher, EpisodeResolved, hub.episode.v1, EventEnvelope, outbound_events
 * WHAT: Converts selected confirmed Hub domain events into versioned NATS envelopes.
 * WHY: Hub owns the persisted fact; bridge and clients must not infer it from the nurse command.
 * WHERE: Listens to Spring domain events emitted after SOR mutations and publishes to NATS.
 */
package com.hub.bridge

import com.fasterxml.jackson.databind.ObjectMapper
import com.hub.shared.domain.DomainEvent
import com.hub.shared.domain.event.SceneConfirmed
import com.manahive.contracts.EventEnvelope
import io.nats.client.Connection
import io.nats.client.Nats
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@ConditionalOnProperty(name = ["nats.enabled"], havingValue = "true")
class NatsDomainEventPublisher(
    private val mapper: ObjectMapper,
    @Value("\${nats.url:nats://localhost:4222}") private val natsUrl: String,
) {
    private var connection: Connection? = null

    // Publish only after persistence commits, so consumers can reconcile the
    // confirmed event through Hub without racing the database transaction.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun publish(event: DomainEvent) {
        val subject = when {
            event is SceneConfirmed -> "hub.scene.v1.${event.bedId.value}"
            event.eventType.startsWith("Episode") -> "hub.episode.v1.${bedId(event) ?: episodeId(event)}"
            else -> return
        }

        val envelope = EventEnvelope(
            eventId = event.eventId,
            type = event.eventType,
            version = 1,
            occurredAt = event.occurredAt,
            source = "mana-hub",
            payloadJson = mapper.writeValueAsString(event),
        )
        val bus = connection ?: runCatching { Nats.connect(natsUrl) }
            .onFailure { failure -> log.warn("Hub NATS connection unavailable: {}", failure.message) }
            .getOrNull()
            ?: return
        connection = bus
        runCatching {
            bus.publish(subject, mapper.writeValueAsBytes(envelope))
            bus.flush(Duration.ofSeconds(2))
        }.onSuccess {
            log.info("Hub NATS event published type={} subject={} eventId={}", event.eventType, subject, event.eventId)
        }.onFailure { failure ->
            log.error("Hub NATS event publish failed type={} subject={}: {}", event.eventType, subject, failure.message)
        }
    }

    @PreDestroy
    fun close() {
        connection?.close()
    }

    private fun episodeId(event: DomainEvent): String =
        mapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(event).path("episodeId").asText()
            .ifBlank { throw IllegalArgumentException("EpisodeResolved event has no episodeId") }

    private fun bedId(event: DomainEvent): String? {
        val node = mapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(event).path("bedId")
        return node.asText().takeIf { it.isNotBlank() }
            ?: node.path("value").asText().takeIf { it.isNotBlank() }
    }

    companion object {
        private val log = LoggerFactory.getLogger(NatsDomainEventPublisher::class.java)
    }
}
