package com.hub.bridge.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.manahive.contracts.EventEnvelope
import io.nats.client.Connection
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

/**
 * Webhook endpoint for mana-hub to notify bridge of profile changes.
 * Bridge then publishes ResidentProfileChanged to NATS.
 */
@RestController
@RequestMapping("/webhooks")
class ProfileChangeWebhook(
    private val connection: Connection,
) {
    private val mapper = ObjectMapper().apply {
        registerModule(JavaTimeModule())
        registerModule(KotlinModule.Builder().build())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    @PostMapping("/profile-change")
    fun onProfileChange(@RequestBody rawJson: String): ResponseEntity<String> {
        try {
            val profile = mapper.readTree(rawJson)
            val residentId = profile.path("residentId").asText("unknown")
            val profileId = profile.path("profileId").asText("unknown")

            val envelope = EventEnvelope(
                eventId = UUID.randomUUID().toString(),
                type = "ResidentProfileChanged",
                version = 1,
                occurredAt = Instant.now(),
                source = "mana-hub",
                payloadJson = mapper.writeValueAsString(mapOf(
                    "at" to Instant.now().toString(),
                    "profile" to profile,
                )),
            )

            // Subject: hub.policy.profile.v1
            connection.publish("hub.policy.profile.v1", mapper.writeValueAsBytes(envelope))
            log.info("Published ResidentProfileChanged for {} ({})", residentId, profileId)

            return ResponseEntity.status(HttpStatus.OK).build()
        } catch (e: Exception) {
            log.error("Failed to publish profile change: {}", e.message)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ProfileChangeWebhook::class.java)
    }
}
