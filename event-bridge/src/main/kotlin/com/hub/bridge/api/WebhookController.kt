package com.hub.bridge.api

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import io.nats.client.Connection
import java.time.Instant
import java.util.UUID

/**
 * Webhook que recibe cambios de políticas de mana-hub y los publica a NATS.
 *
 * mana-hub hace POST aquí cuando cambia un preset.
 * El bridge publica a NATS para que mana-hibe lo reciba.
 */
@RestController
@RequestMapping("/webhooks")
class WebhookController(
    private val connection: Connection,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/policy-change")
    fun onPolicyChange(@RequestBody payload: Map<String, Any>): ResponseEntity<Map<String, Any>> {
        val residentId = payload["residentId"] as? String
        if (residentId.isNullOrBlank()) {
            return ResponseEntity.badRequest().body(mapOf("error" to "missing or blank residentId"))
        }

        val snapshot = payload["snapshot"]
        if (snapshot == null || snapshot !is Map<*, *>) {
            return ResponseEntity.badRequest().body(mapOf("error" to "missing or invalid snapshot"))
        }

        val fingerprint = payload["fingerprint"] as? String
        if (fingerprint.isNullOrBlank()) {
            return ResponseEntity.badRequest().body(mapOf("error" to "missing or blank fingerprint"))
        }

        val event = mapOf(
            "eventId" to UUID.randomUUID().toString(),
            "type" to "PolicyChangeDetected",
            "version" to 1,
            "occurredAt" to Instant.now().toString(),
            "source" to "mana-hub",
            "payloadJson" to objectMapper.writeValueAsString(payload),
        )

        val js = connection.jetStream()
        val subject = "hub.policy.change.v1"
        val data = objectMapper.writeValueAsBytes(event)

        js.publish(subject, data)

        log.info("Published policy change for resident {}", residentId)
        return ResponseEntity.ok(mapOf("status" to "published", "residentId" to residentId))
    }
}
