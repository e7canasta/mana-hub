package com.hub.integration.infrastructure.bridge

import com.hub.integration.application.service.ProfileChangedEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * Listens for ProfileChangedEvent and notifies the bridge via webhook.
 * Bridge then publishes ResidentProfileChanged to NATS for mana-hive.
 */
@Component
class BridgeProfileNotifier(
    @Value("\${bridge.target.url:http://localhost:8090}") private val bridgeUrl: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client: RestClient = RestClient.builder().baseUrl(bridgeUrl).build()

    @EventListener
    fun onProfileChanged(event: ProfileChangedEvent) {
        try {
            client.post()
                .uri("/webhooks/profile-change")
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
