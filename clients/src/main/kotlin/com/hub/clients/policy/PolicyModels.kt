package com.hub.clients.policy

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class AlarmProfileResponse(
    val id: String,
    @JsonProperty("residentId") val residentId: String,
    @JsonProperty("validFrom") val validFrom: Instant,
    @JsonProperty("validTo") val validTo: Instant? = null,
    @JsonProperty("mobilityAid") val mobilityAid: String? = null,
    val autopilot: Boolean = false,
    val mode: String? = null,
    @JsonProperty("templateId") val templateId: String? = null,
    @JsonProperty("overridesJson") val overridesJson: String = "{}",
    @JsonProperty("riskLevel") val riskLevel: RiskLevel = RiskLevel.MEDIUM,
    @JsonProperty("current") val isCurrent: Boolean = true
)

data class UpdateAlarmProfileRequest(
    @JsonProperty("mobilityAid") val mobilityAid: String? = null,
    val autopilot: Boolean? = null,
    val mode: String? = null,
    @JsonProperty("templateId") val templateId: String? = null,
    @JsonProperty("overridesJson") val overridesJson: String? = null,
    @JsonProperty("riskLevel") val riskLevel: RiskLevel? = null,
    @JsonProperty("updatedBy") val updatedBy: String? = null
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

enum class RiskLevel { LOW, MEDIUM, HIGH }

// ══════════════════════════════════════════════════════════════
//  MONITORING PROFILE — Vocabulary alias for AlarmProfile
// ══════════════════════════════════════════════════════════════

typealias MonitoringProfile = AlarmProfileResponse
typealias MonitoringProfileUpdateRequest = UpdateAlarmProfileRequest
