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
                log.info("ROUTING SceneEvent type={} subject={}", type, subject)
                forward("/internal/v1/integration/scene-events", payload, type, subject)
            }
            subject.startsWith("sentinel.") -> {
                log.info("ROUTING SentinelSignal type={} subject={}", type, subject)
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
        val callId = java.util.UUID.randomUUID().toString().take(8)
        log.info("FORWARD CALL #{} type={} path={} subject={}", callId, type, path, subject)
        try {
            val response = client.post()
                .uri(path)
                .header("Content-Type", "application/json")
                .body(payload)
                .retrieve()
                .toEntity(String::class.java)

            log.info("FORWARD OK #{} type={} status={} body={}", callId, type, response.statusCode, response.body?.take(50) ?: "empty")
        } catch (e: Exception) {
            log.error("FORWARD FAIL #{} type={} path={}: {}", callId, type, path, e.message)
        }
    }
}
