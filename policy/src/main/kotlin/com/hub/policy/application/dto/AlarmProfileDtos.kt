package com.hub.policy.application.dto

import com.hub.policy.domain.model.*
import java.time.Duration
import java.time.Instant

// ── Catalog DTOs ──────────────────────────────────────────────────

data class AlarmPresetCatalogResponse(
    val levels: List<String>,
    val mobilityAids: List<String>,
    val actions: List<String>,
    val shifts: List<String>,
    val modes: List<String>,
    val sensitivities: List<String>,
    val groups: List<AlarmGroupDto>,
    val transitions: List<AlarmTransitionDto>,
    val presets: Map<String, Map<String, AlarmPresetRuleDto>>,
    val templates: List<AlarmTemplateDto>,
    val riskFactors: List<RiskFactorDto>,
    val autopilot: AutopilotConfigDto?,
)

data class AlarmGroupDto(
    val id: String,
    val label: String,
    val detail: String?,
)

data class AlarmTransitionDto(
    val id: String,
    val group: String,
    val label: String,
    val shortLabel: String?,
    val detail: String?,
    val pictogram: String?,
    val art: String,
    val locked: Boolean,
    val requiresAid: List<String>?,
    val params: List<AlarmParamDto>,
)

data class AlarmParamDto(
    val id: String,
    val type: String,
    val label: String,
    val unit: String?,
    val min: Number?,
    val max: Number?,
    val step: Number?,
)

data class AlarmPresetRuleDto(
    val day: String,
    val night: String,
    val params: Map<String, Any>?,
)

data class AlarmTemplateDto(
    val id: String,
    val label: String,
    val detail: String?,
    val recommendedFor: List<String>,
    val rules: Map<String, Any>,
)

data class RiskFactorDto(
    val id: String,
    val label: String,
    val icon: String?,
)

data class AutopilotConfigDto(
    val minimumSignalsForRaise: Int,
    val minimumDaysBetweenChanges: Int,
)

// ── Profile Response DTOs ─────────────────────────────────────────

data class AlarmProfileResponse(
    val resident: AlarmProfileResidentDto,
    val profile: AlarmProfileSettingsDto,
    val effective: AlarmEffectiveDto,
    val recommendation: AlarmRecommendationDto,
)

data class AlarmProfileResidentDto(
    val id: String,
    val fullName: String,
    val externalId: String? = null,
    val roomNumber: String? = null,
    val bedLabel: String? = null,
    val monitorKey: String? = null,
    val wingId: String? = null,
    val wingName: String? = null,
    val traits: List<String>,
)

data class AlarmProfileSettingsDto(
    val riskLevel: String,
    val mobilityAid: String,
    val autopilot: Boolean,
    val mode: String,
    val templateId: String,
    val overrides: Map<String, Any>,
    val updatedAt: String?,
    val updatedBy: String?,
    val updatedByName: String?,
    val source: String,
)

data class AlarmEffectiveDto(
    val level: String,
    val mobilityAid: String,
    val mode: String,
    val templateId: String,
    val rules: Map<String, AlarmRuleDto>,
)

data class AlarmRuleDto(
    val day: String,
    val night: String,
    val params: Map<String, Any>?,
)

data class AlarmRecommendationDto(
    val level: String,
    val changed: Boolean,
    val factors: List<RiskFactorDto>,
    val score: Int,
    val signalsEvaluated: Int,
    val suggestedTemplate: String,
    val computedAt: String,
)

// ── Request DTOs ──────────────────────────────────────────────────

data class UpdateAlarmProfileRequest(
    val mobilityAid: String? = null,
    val autopilot: Boolean? = null,
    val mode: String? = null,
    val templateId: String? = null,
    val overridesJson: String? = null,
    val riskLevel: String? = null,
    val updatedBy: String? = null,
    val reason: String? = null,
)
