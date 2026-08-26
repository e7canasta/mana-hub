package com.hub.policy.application.dto

import com.hub.policy.domain.model.RiskLevel
import java.time.Instant

data class AlarmProfileResponse(
    val id: String,
    val residentId: String,
    val validFrom: Instant,
    val validTo: Instant?,
    val mobilityAid: String?,
    val autopilot: Boolean,
    val mode: String?,
    val templateId: String?,
    val overridesJson: String,
    val riskLevel: RiskLevel,
    val isCurrent: Boolean
)

data class UpdateAlarmProfileRequest(
    val mobilityAid: String? = null,
    val autopilot: Boolean? = null,
    val mode: String? = null,
    val templateId: String? = null,
    val overridesJson: String? = null,
    val riskLevel: RiskLevel? = null,
    val updatedBy: String? = null
)

data class AlarmPresetCatalogResponse(
    val presets: List<AlarmPresetDefinition>
)

data class AlarmPresetDefinition(
    val id: String,
    val name: String,
    val description: String,
    val thresholds: Map<String, Any>
)

data class ApplyRecommendationsRequest(
    val residentId: String,
    val recommendations: List<String>
)

data class AutopilotRequest(
    val residentId: String,
    val enabled: Boolean
)
