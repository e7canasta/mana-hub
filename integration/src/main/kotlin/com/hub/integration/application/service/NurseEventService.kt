package com.hub.integration.application.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.hub.shared.domain.port.EpisodeNotePort
import com.hub.shared.domain.port.EpisodePort
import com.hub.shared.domain.port.CreateEpisodeNotePortRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Processes nurse action events from murmur (station companion).
 *
 * These events arrive via NATS → event-bridge → POST /internal/v1/integration/nurse-events.
 * The nurse INFORMS; mana-hub PERSISTS. The engine (mana-hive) decides whether to close.
 */
@Service
class NurseEventService(
    private val episodePort: EpisodePort,
    private val episodeNotePort: EpisodeNotePort,
    private val objectMapper: ObjectMapper,
) {

    @Transactional
    fun processNurseEvent(body: String) {
        val tree = objectMapper.readTree(body)
        val type = tree.get("type")?.asText() ?: "unknown"
        val payloadJson = tree.get("payloadJson")

        log.info("Nurse event received: type={}", type)

        when {
            type == "nurse.ack.v1" -> handleAck(payloadJson)
            type == "nurse.resolved.v1" -> handleResolved(payloadJson)
            type == "nurse.episode_note.v1" -> handleEpisodeNote(payloadJson)
            type == "nurse.camera.v1" -> handleCamera(payloadJson)
            type == "nurse.mute.v1" -> handleMute(payloadJson)
            type == "nurse.pause.v1" -> handlePause(payloadJson)
            else -> log.warn("Unknown nurse event type: {}", type)
        }
    }

    private fun handleAck(payload: JsonNode?) {
        if (payload == null) return
        val episodeId = payload.get("episode")?.asText() ?: return
        val stationId = payload.get("stationId")?.asText() ?: "unknown"

        log.info("Nurse ACK: episode={} station={}", episodeId, stationId)
        try {
            episodePort.acknowledgeEpisode(episodeId, stationId)
        } catch (e: Exception) {
            log.error("Failed to acknowledge episode {}: {}", episodeId, e.message)
        }
    }

    private fun handleResolved(payload: JsonNode?) {
        if (payload == null) return
        val episodeId = payload.get("episode")?.asText() ?: return
        val stationId = payload.get("stationId")?.asText() ?: "unknown"

        log.info("Nurse RESOLVED: episode={} station={}", episodeId, stationId)
        try {
            episodePort.resolveEpisode(episodeId, stationId)
        } catch (e: Exception) {
            log.error("Failed to resolve episode {}: {}", episodeId, e.message)
        }
    }

    private fun handleEpisodeNote(payload: JsonNode?) {
        if (payload == null) return
        val episodeId = payload.get("episode")?.asText() ?: return
        val note = payload.get("note")?.asText() ?: return
        val stationId = payload.get("stationId")?.asText() ?: "unknown"

        log.info("Nurse NOTE: episode={} station={}", episodeId, stationId)
        try {
            episodeNotePort.createNote(
                CreateEpisodeNotePortRequest(
                    episodeId = episodeId,
                    authorId = stationId,
                    kind = "CLINICAL_NOTE",
                    body = note,
                )
            )
        } catch (e: Exception) {
            log.error("Failed to create episode note for {}: {}", episodeId, e.message)
        }
    }

    private fun handleCamera(payload: JsonNode?) {
        if (payload == null) return
        val episodeId = payload.get("episode")?.asText() ?: "unknown"
        val durationMs = payload.get("durationMs")?.asLong() ?: 0
        val detail = payload.get("detail")?.asText() ?: "LOW"
        log.info("Nurse CAMERA: episode={} durationMs={} detail={}", episodeId, durationMs, detail)
        // Audit only — camera is handled by the recorder, not hub
    }

    private fun handleMute(payload: JsonNode?) {
        if (payload == null) return
        val muted = payload.get("muted")?.asBoolean() ?: false
        val stationId = payload.get("stationId")?.asText() ?: "unknown"
        log.info("Nurse MUTE: muted={} station={}", muted, stationId)
        // Audit only — mute is local to the station
    }

    private fun handlePause(payload: JsonNode?) {
        if (payload == null) return
        val paused = payload.get("paused")?.asBoolean() ?: false
        val stationId = payload.get("stationId")?.asText() ?: "unknown"
        log.info("Nurse PAUSE: paused={} station={}", paused, stationId)
        // Audit only — pause is local to the station
    }

    companion object {
        private val log = LoggerFactory.getLogger(NurseEventService::class.java)
    }
}
