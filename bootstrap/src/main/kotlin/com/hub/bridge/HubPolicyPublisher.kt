package com.hub.bridge

import com.hub.policy.domain.model.AlarmProfileVersion
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import java.util.UUID

/**
 * Publica cambios de preset al bridge vía webhook.
 *
 * mana-hub solo hace POST al bridge. El bridge se encarga de NATS.
 * RestClient (Spring 6.1+, bloqueante) reemplaza a WebClient reactivo:
 * mismo contrato, sin Reactor/Netty → -~3k clases para Leyden.
 */
@Service
class HubPolicyPublisher(
    @Value("\${bridge.webhook.url:http://localhost:8090/webhooks/policy-change}") private val webhookUrl: String,
) {
    private val client = RestClient.builder().build()

    @Transactional
    fun publishChange(
        version: AlarmProfileVersion,
        fingerprint: String = UUID.randomUUID().toString()
    ) {
        val payload = mapOf(
            "residentId" to version.residentId.value,
            "snapshot" to mapOf(
                "templateId" to version.templateId?.value,
                "riskLevel" to version.riskLevel.name.lowercase(),
                "mobilityAid" to version.mobilityAid?.name?.lowercase(),
                "autopilot" to version.autopilot,
                "mode" to version.mode?.name?.lowercase(),
            ),
            "fingerprint" to fingerprint,
        )

        try {
            client.post()
                .uri(webhookUrl)
                .header("Content-Type", "application/json")
                .body(com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload))
                .retrieve()
                .body(String::class.java)
            log.info("Published policy change for resident {} via webhook", version.residentId.value)
        } catch (e: Exception) {
            log.error("Webhook failed for resident {}: {}", version.residentId.value, e.message)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(HubPolicyPublisher::class.java)
    }
}
