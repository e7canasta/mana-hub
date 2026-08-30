package com.hub.integration.infrastructure.bridge

import com.hub.integration.application.service.ProfileChangedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * Listens for ProfileChangedEvent and notifies the bridge via webhook.
 * Bridge then publishes ResidentProfileChanged to NATS for mana-hive.
 */
@Component
class BridgeProfileNotifier(
    private val client: RestClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener
    fun onProfileChanged(event: ProfileChangedEvent) {
        try {
            client.post()
                .uri("http://localhost:8090/webhooks/profile-change")
                .header("Content-Type", "application/json")
                .body(event.rawJson)
                .retrieve()
                .body(String::class.java)
            log.info("Bridge notified of profile change for {}", event.residentId)
        } catch (e: Exception) {
            log.error("Failed to notify bridge for {}: {}", event.residentId, e.message)
        }
    }
}
