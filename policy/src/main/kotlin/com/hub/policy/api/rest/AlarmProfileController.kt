package com.hub.policy.api.rest

import com.hub.policy.application.dto.*
import com.hub.policy.application.service.AlarmProfileApplicationService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/alarm-presets")
class AlarmProfileController(
    private val alarmProfileApplicationService: AlarmProfileApplicationService
) {

    @GetMapping("/catalog")
    fun getCatalog(): ResponseEntity<AlarmPresetCatalogResponse> {
        return ResponseEntity.ok(alarmProfileApplicationService.getCatalog())
    }

    @GetMapping
    fun listProfiles(@RequestParam residentId: String?): ResponseEntity<List<AlarmProfileResponse>> {
        return if (residentId != null) {
            val profile = alarmProfileApplicationService.getResidentProfile(residentId)
            ResponseEntity.ok(profile?.let { listOf(it) } ?: emptyList())
        } else {
            ResponseEntity.ok(emptyList())
        }
    }

    @GetMapping("/{residentId}")
    fun getResidentProfile(@PathVariable residentId: String): ResponseEntity<AlarmProfileResponse> {
        val profile = alarmProfileApplicationService.getResidentProfile(residentId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(profile)
    }

    @PatchMapping("/{residentId}")
    fun updateResidentProfile(
        @PathVariable residentId: String,
        @Valid @RequestBody request: UpdateAlarmProfileRequest
    ): ResponseEntity<AlarmProfileResponse> {
        return ResponseEntity.ok(alarmProfileApplicationService.updateResidentProfile(residentId, request))
    }

    @GetMapping("/{residentId}/history")
    fun getProfileHistory(@PathVariable residentId: String): ResponseEntity<List<AlarmProfileResponse>> {
        return ResponseEntity.ok(alarmProfileApplicationService.getProfileHistory(residentId))
    }
}
