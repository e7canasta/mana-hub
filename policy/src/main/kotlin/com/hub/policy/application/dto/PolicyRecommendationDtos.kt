package com.hub.policy.application.dto

data class PolicyRecommendationDto(
    val id: String,
    val episodeId: String,
    val residentId: String,
    val title: String,
    val description: String,
    val presetPatch: PresetPatchDto,
    val origin: String,
    val state: String,
    val createdAt: String,
    val resolvedAt: String?,
    val appliedAt: String?,
)

data class PresetPatchDto(
    val templateId: String?,
    val mode: String?,
    val riskLevel: String?,
    val mobilityAid: String?,
    val autopilot: Boolean?,
)
