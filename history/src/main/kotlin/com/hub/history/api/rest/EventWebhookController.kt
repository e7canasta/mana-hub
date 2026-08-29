package com.hub.history.api.rest

import com.hub.history.application.service.EpisodeTimelineBuilder
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

/**
 * Webhook que recibe eventos del bridge.
 *
 * El bridge hace POST aquí cuando llega un evento de mana-hibe.
 * mana-hub procesa y persiste.
 */
@RestController
@RequestMapping("/webhooks")
class EventWebhookController(
    private val timelineBuilder: EpisodeTimelineBuilder,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/events")
    fun onEvent(@RequestBody payload: Map<String, Any>): ResponseEntity<Map<String, Any>> {
        val eventId = payload["eventId"] as? String
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "missing eventId"))

        val type = payload["type"] as? String
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "missing type"))

        val subject = payload["subject"] as? String
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "missing subject"))

        val occurredAt = payload["occurredAt"] as? String
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "missing occurredAt"))

        val payloadJson = payload["payloadJson"] as? String ?: "{}"

        log.info("Received event {} type={} from {}", eventId, type, subject)

        when (subject) {
            "sentinel" -> processSentinelEvent(eventId, type, payloadJson, occurredAt)
            "scene" -> processSceneEvent(eventId, payloadJson, occurredAt)
            "alarm" -> processAlarmEvent(eventId, type, payloadJson, occurredAt)
            else -> log.debug("Ignoring event from subject {}", subject)
        }

        return ResponseEntity.ok(mapOf("status" to "processed", "eventId" to eventId))
    }

    private fun processSentinelEvent(eventId: String, type: String, payloadJson: String, occurredAt: String) {
        val node = com.fasterxml.jackson.databind.ObjectMapper().readTree(payloadJson)
        val residentId = node.get("residentId")?.asText() ?: return
        val episodeId = node.get("episodeId")?.asText() ?: eventId
        val bedId = node.get("bedId")?.asText()
        val severity = node.get("severity")?.asText()
        val title = node.get("title")?.asText() ?: "Sentinel: $type"

        timelineBuilder.onSentinelEvent(
            eventId = eventId,
            eventType = type,
            episodeId = episodeId,
            residentId = residentId,
            bedId = bedId,
            severity = severity,
            fromState = node.get("fromState")?.asText(),
            toState = node.get("toState")?.asText(),
            description = title,
            occurredAt = Instant.parse(occurredAt),
        )
    }

    private fun processSceneEvent(eventId: String, payloadJson: String, occurredAt: String) {
        val node = com.fasterxml.jackson.databind.ObjectMapper().readTree(payloadJson)
        val episodeId = node.get("episodeId")?.asText()

        timelineBuilder.onSceneEvent(
            eventId = eventId,
            episodeId = episodeId,
            residentId = node.get("residentId")?.asText(),
            bedId = node.get("bedId")?.asText(),
            fromState = node.get("fromState")?.asText(),
            toState = node.get("toState")?.asText(),
            triggerType = node.get("triggerType")?.asText(),
            occurredAt = Instant.parse(occurredAt),
        )
    }

    private fun processAlarmEvent(eventId: String, type: String, payloadJson: String, occurredAt: String) {
        val node = com.fasterxml.jackson.databind.ObjectMapper().readTree(payloadJson)

        timelineBuilder.onAlarmEvent(
            eventId = eventId,
            episodeId = node.get("episodeId")?.asText(),
            residentId = node.get("residentId")?.asText(),
            channel = node.get("channel")?.asText(),
            severity = node.get("severity")?.asText(),
            eventType = type,
            occurredAt = Instant.parse(occurredAt),
        )
    }
}
