package com.hub.integration.api.rest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.hub.policy.application.dto.UpdateAlarmProfileRequest
import com.hub.policy.application.service.AlarmProfileApplicationService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Policy integration endpoint — mana-hive AlarmProfile → mana-hub persistence.
 *
 * Accepts PolicyChangeDetected from the bus and persists the AlarmProfile.
 * When the profile changes, HubPolicyBridgeListener notifies the bridge,
 * which retransmits to NATS for mana-hive to consume.
 */
@RestController
@RequestMapping("/internal/v1/integration")
class PolicyIntegrationController(
    private val alarmProfileService: AlarmProfileApplicationService,
) {
    private val objectMapper = ObjectMapper()

    /**
     * Receive PolicyChangeDetected from mana-hive and persist the AlarmProfile.
     *
     * Flow:
     * 1. mana-hive publishes PolicyChangeDetected to hub.policy.change.v1
     * 2. Bridge forwards to this endpoint
     * 3. We persist the profile
     * 4. HubPolicyBridgeListener fires → bridge retransmits to NATS
     */
    @PostMapping("/policy-changes")
    fun ingestPolicyChange(@RequestBody body: String): ResponseEntity<String> {
        val tree = objectMapper.readTree(body)
        val residentId = tree.path("residentId").path("value").asText("unknown")
        val snapshot = tree.path("snapshot")

        log.info("PolicyChangeDetected received for resident {}", residentId)

        val request = UpdateAlarmProfileRequest(
            riskLevel = snapshot.path("riskLevel").asText("MEDIUM"),
            mobilityAid = snapshot.path("mobilityAid").asText("none"),
            autopilot = snapshot.path("autopilot").asBoolean(false),
            mode = snapshot.path("mode").asText("preset"),
            templateId = snapshot.path("templateId").path("value").asText(null),
            updatedBy = "integration",
        )

        alarmProfileService.updateResidentProfile(residentId, request)
        log.info("Profile updated for resident {} from PolicyChangeDetected", residentId)

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    companion object {
        private val log = LoggerFactory.getLogger(PolicyIntegrationController::class.java)
    }
}
