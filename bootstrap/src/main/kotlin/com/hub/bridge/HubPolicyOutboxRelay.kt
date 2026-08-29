package com.hub.bridge

import com.manahive.messaging.BusEvents
import com.manahive.messaging.Subjects
import io.nats.client.JetStream
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Relay outbox → JetStream.
 * Poll cada 500ms, publica a hub.policy.change.v1 y marca published.
 * Si JetStream no existe, crea stream vía NatsTopology (ya hecho por NatsClientConfiguration).
 * Reutiliza BusEvents pattern de hive: publica solo cuando conectado.
 *
 * Nota: si nats.enabled=false, este componente no se crea.
 */
@Component
@ConditionalOnProperty(name = ["nats.enabled"], havingValue = "true")
class HubPolicyOutboxRelay(
    private val outboxRepository: HubPolicyOutboxRepository,
    private val events: BusEvents,
    @Value("\${nats.enabled:true}") private val enabled: Boolean
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 500)
    fun pollAndPublish() {
        if (!enabled) return
        if (!events.connected) {
            log.debug("Bus not connected, outbox polling paused ({} unpublished)", outboxRepository.findUnpublished().size)
            return
        }
        val batch = outboxRepository.findUnpublished()
        if (batch.isEmpty()) return
        val js = try { events.connection!!.jetStream() } catch (e: Exception) {
            log.warn("JetStream unavailable, retry next poll: {}", e.message)
            return
        }
        for (msg in batch) {
            try {
                val subject = when (msg.type) {
                    "PolicyChangeDetected" -> Subjects.policyChangeDetected()
                    "EffectiveRules" -> Subjects.effectiveRules(msg.aggregateId)
                    else -> Subjects.policyChangeDetected()
                }
                val data = msg.payloadJson.toByteArray(Charsets.UTF_8)
                // NATS header: Nats-Msg-Id = eventId para deduplicación 10min (duplicateWindow)
                val headers = io.nats.client.impl.Headers().apply { put("Nats-Msg-Id", msg.id) }
                js.publish(subject, headers, data)
                msg.published = true
                msg.attempts += 1
                outboxRepository.save(msg)
                log.debug("Published outbox {} to {}", msg.id, subject)
            } catch (e: Exception) {
                msg.attempts += 1
                msg.lastError = e.message?.take(500)
                outboxRepository.save(msg)
                log.error("Failed to publish outbox {}: {}", msg.id, e.message)
            }
        }
    }
}
