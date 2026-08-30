package com.hub.bridge.ingest

import com.fasterxml.jackson.databind.ObjectMapper
import com.manahive.contracts.EventEnvelope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * Routes deserialized events from mana-hive to the correct mana-hub endpoint.
 *
 * Shared Kernel: uses the same types from contracts JAR.
 * No translation — forward to the right endpoint.
 */
@Component
class EventRouter(
    private val objectMapper: ObjectMapper,
    private val client: RestClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun route(envelope: EventEnvelope, subject: String) {
        val type = envelope.type
        val payload = envelope.payloadJson

        when {
            // Perception → POST /internal/v1/events
            subject.startsWith("perception.") -> {
                forward("/internal/v1/events", payload, type, subject)
            }

            // Scene → POST /internal/v1/scene-events
            subject.startsWith("scene.") -> {
                forward("/internal/v1/scene-events", payload, type, subject)
            }

            // Sentinel → POST /api/v1/episodes (EpisodeOpened) or skip
            subject.startsWith("sentinel.") -> {
                when (type) {
                    "EpisodeOpened" -> forward("/api/v1/episodes", payload, type, subject)
                    "EpisodeClosed" -> forwardEpisodePatch(payload, type, subject)
                    else -> log.debug("Skipping sentinel type: {} on {}", type, subject)
                }
            }

            // Alarm → POST /internal/v1/notifications
            subject.startsWith("alarm.") -> {
                forward("/internal/v1/notifications", payload, type, subject)
            }

            // Recorder → POST /internal/v1/recordings (TODO: endpoint not yet in hub)
            subject.startsWith("recorder.") -> {
                log.info("Recorder event: {} on {} — hub has no endpoint yet", type, subject)
            }

            // Evidence → POST /internal/v1/evidence (TODO: endpoint not yet in hub)
            subject.startsWith("evidence.") -> {
                log.info("Evidence event: {} on {} — hub has no endpoint yet", type, subject)
            }

            else -> {
                log.debug("Unknown subject pattern: {} — skipping", subject)
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

            log.debug("Forwarded {} → {} from {}", type, path, subject)
        } catch (e: Exception) {
            log.error("Failed to forward {} to {}: {}", type, path, e.message)
        }
    }

    private fun forwardEpisodePatch(payload: String, type: String, subject: String) {
        try {
            val tree = objectMapper.readTree(payload)
            val episodeId = tree.get("episode")?.asText()
                ?: tree.get("episodeId")?.asText()
                ?: run {
                    log.warn("EpisodeClosed without episode id on {}", subject)
                    return
                }

            client.patch()
                .uri("/api/v1/episodes/$episodeId")
                .header("Content-Type", "application/json")
                .body(mapOf("status" to "RESOLVED"))
                .retrieve()
                .body(String::class.java)

            log.debug("Forwarded {} → PATCH /api/v1/episodes/{}/status from {}", type, episodeId, subject)
        } catch (e: Exception) {
            log.error("Failed to forward EpisodeClosed: {}", e.message)
        }
    }
}
