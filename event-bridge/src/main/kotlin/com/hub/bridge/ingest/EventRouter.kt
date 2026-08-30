package com.hub.bridge.ingest

import com.manahive.contracts.EventEnvelope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * Routes raw events from mana-hive to mana-hub integration endpoints.
 *
 * Shared Kernel: bridge validates subject pattern, forwards raw JSON.
 * No deserialization — mana-hub handles the JSON directly.
 */
@Component
class EventRouter(
    private val client: RestClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun route(envelope: EventEnvelope, subject: String) {
        val type = envelope.type
        val payload = envelope.payloadJson

        when {
            subject.startsWith("scene.") -> {
                forward("/internal/v1/integration/scene-events", payload, type, subject)
            }
            subject.startsWith("sentinel.") -> {
                forward("/internal/v1/integration/signal-events", payload, type, subject)
            }
            subject.startsWith("perception.") -> {
                log.debug("Skipping perception: {} on {}", type, subject)
            }
            subject.startsWith("alarm.") -> {
                log.debug("Skipping alarm: {} on {}", type, subject)
            }
            subject.startsWith("hub.") -> {
                log.debug("Skipping hub-originated: {} on {}", type, subject)
            }
            subject.startsWith("recorder.") -> {
                log.debug("Skipping recorder: {} on {}", type, subject)
            }
            subject.startsWith("evidence.") -> {
                log.debug("Skipping evidence: {} on {}", type, subject)
            }
            else -> {
                log.debug("Unknown subject: {} — skipping", subject)
            }
        }
    }

    private fun forward(path: String, payload: String, type: String, subject: String) {
        try {
            client.post()
                .uri(path)
                .header("Content-Type", "application/json")
                .body(payload)
                .retrieve()
                .body(String::class.java)

            log.info("Forwarded {} → {} from {}", type, path, subject)
        } catch (e: Exception) {
            log.error("Failed to forward {} to {}: {}", type, path, e.message)
        }
    }
}
