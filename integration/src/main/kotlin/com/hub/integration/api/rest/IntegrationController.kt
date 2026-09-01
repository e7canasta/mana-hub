package com.hub.integration.api.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.hub.integration.application.service.IntegrationService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Integration endpoints — mana-hive bus events → mana-hub persistence.
 *
 * Shared Kernel: accepts contracts JAR types via raw JSON string,
 * deserializes with contracts-aware mapper.
 */
@RestController
@RequestMapping("/internal/v1/integration")
class IntegrationController(
    private val integrationService: IntegrationService,
) {
    private val objectMapper = ObjectMapper()

    @PostMapping("/scene-events")
    fun ingestSceneEvent(@RequestBody body: String): ResponseEntity<String> {
        val tree = objectMapper.readTree(body)
        val type = tree.get("type") ?: "unknown"
        log.info("SceneEvent received: {} body.length={}", type, body.length)
        integrationService.ingestSceneEvent(tree)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/signal-events")
    fun ingestSignalEvent(@RequestBody body: String): ResponseEntity<String> {
        val tree = objectMapper.readTree(body)
        val type = tree.get("type") ?: "unknown"
        log.info("SentinelSignal received: {} body.length={}", type, body.length)
        integrationService.ingestSignalEvent(tree)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    companion object {
        private val log = LoggerFactory.getLogger(IntegrationController::class.java)
    }
}
