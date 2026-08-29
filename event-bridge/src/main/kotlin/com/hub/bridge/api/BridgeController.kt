package com.hub.bridge.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * API del bridge — health check y status.
 *
 * Los eventos ya no se almacenan en el bridge.
 * El bridge los hace POST a mana-hub directamente.
 */
@RestController
@RequestMapping("/api/v1/bridge")
class BridgeController {

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(mapOf(
            "status" to "UP",
            "service" to "event-bridge",
            "mode" to "push (no storage)",
        ))
    }
}
