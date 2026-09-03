package com.hub.integration.api.rest

import com.hub.integration.application.service.InvalidResidentProfileException
import com.hub.integration.application.service.ResidentProfileService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.*

/**
 * Profile integration endpoint — mana-hive ResidentProfileDto → mana-hub.
 *
 * Accepts the EXACT same ResidentProfileDto that mana-hive publishes
 * on hub.policy.profile.v1. Persists and notifies bridge.
 */
@RestController
@RequestMapping("/api/profiles")
class ResidentProfileController(
    private val profileService: ResidentProfileService,
) {

    /**
     * Publish a full ResidentProfileDto.
     * PUT /api/profiles/{residentId}
     *
     * Body: the exact ResidentProfileDto JSON from contracts JAR.
     */
    @PutMapping("/{residentId}")
    fun publishProfile(
        @PathVariable residentId: String,
        @RequestBody body: String,
    ): ResponseEntity<String> {
        log.info("ResidentProfile received for {}", residentId)
        val profile = profileService.ingestProfile(residentId, body)
        log.info("Profile persisted: {} v{}", profile.profileId, profile.version)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @ExceptionHandler(InvalidResidentProfileException::class)
    fun invalidProfile(e: InvalidResidentProfileException): ResponseEntity<String> =
        ResponseEntity.badRequest().body(e.message ?: "Invalid ResidentProfileDto")

    /**
     * Get current profile for a resident.
     * GET /api/profiles/{residentId}
     */
    @GetMapping("/{residentId}")
    fun getCurrentProfile(@PathVariable residentId: String): ResponseEntity<String> {
        val profile = profileService.getCurrent(residentId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(profile.rawJson)
    }

    /**
     * Get all active profiles (cold boot).
     * GET /api/profiles?active=true
     */
    @GetMapping
    fun getActiveProfiles(
        @RequestParam(required = false) active: Boolean?,
    ): ResponseEntity<List<String>> {
        val profiles = profileService.getActiveProfiles()
        return ResponseEntity.ok(profiles.map { it.rawJson })
    }

    companion object {
        private val log = LoggerFactory.getLogger(ResidentProfileController::class.java)
    }
}
