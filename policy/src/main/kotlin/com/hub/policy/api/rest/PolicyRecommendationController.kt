package com.hub.policy.api.rest

import com.hub.policy.application.dto.PolicyRecommendationDto
import com.hub.policy.application.dto.PresetPatchDto
import com.hub.policy.application.service.PolicyRecommendationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/residents/{residentId}/alarm-presets")
class PolicyRecommendationController(
    private val service: PolicyRecommendationService,
) {

    @GetMapping("/recommendations")
    fun getRecommendations(
        @PathVariable residentId: String,
        @RequestParam(defaultValue = "PENDING") state: String,
    ): ResponseEntity<List<PolicyRecommendationDto>> {
        val recommendations = when (state) {
            "PENDING" -> service.getPendingByResident(residentId)
            else -> emptyList()
        }
        return ResponseEntity.ok(recommendations.map { it.toDto() })
    }

    @PatchMapping("/recommendations/{recommendationId}/approve")
    fun approve(
        @PathVariable residentId: String,
        @PathVariable recommendationId: String,
    ): ResponseEntity<PolicyRecommendationDto> {
        return ResponseEntity.ok(service.approve(recommendationId).toDto())
    }

    @PatchMapping("/recommendations/{recommendationId}/discard")
    fun discard(
        @PathVariable residentId: String,
        @PathVariable recommendationId: String,
    ): ResponseEntity<PolicyRecommendationDto> {
        return ResponseEntity.ok(service.discard(recommendationId).toDto())
    }

    private fun com.hub.policy.domain.model.recommendation.PolicyRecommendation.toDto() = PolicyRecommendationDto(
        id = id.value.toString(),
        episodeId = episodeId,
        residentId = residentId.value,
        title = title,
        description = description,
        presetPatch = PresetPatchDto(
            templateId = presetPatch.templateId?.value,
            mode = presetPatch.mode?.name?.lowercase(),
            riskLevel = presetPatch.riskLevel?.name?.lowercase(),
            mobilityAid = presetPatch.mobilityAid?.name?.lowercase(),
            autopilot = presetPatch.autopilot,
        ),
        origin = origin.name,
        state = state.name,
        createdAt = createdAt.toString(),
        resolvedAt = resolvedAt?.toString(),
        appliedAt = appliedAt?.toString(),
    )
}
