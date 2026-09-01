package com.hub.policy.application.service

import com.hub.policy.application.dto.*
import com.hub.policy.domain.model.*
import org.springframework.stereotype.Component

@Component
class AlarmProfileResponseBuilder {

    fun buildResponse(
        version: AlarmProfileVersion,
        residentId: String,
        watchLevel: WatchLevel,
        catalog: DagCatalog,
        overridesMap: Map<String, Any>,
        traits: List<String> = emptyList(),
    ): AlarmProfileResponse {
        return AlarmProfileResponse(
            resident = AlarmProfileResidentDto(
                id = residentId,
                fullName = "",
                traits = traits,
            ),
            profile = AlarmProfileSettingsDto(
                riskLevel = version.riskLevel.name.lowercase(),
                mobilityAid = version.mobilityAid?.name?.lowercase() ?: "none",
                autopilot = version.autopilot,
                mode = version.mode?.name?.lowercase() ?: "preset",
                templateId = version.templateId?.value ?: watchLevel.name.lowercase(),
                overrides = overridesMap,
                updatedAt = version.validFrom.toString(),
                updatedBy = version.updatedBy,
                updatedByName = null,
                source = if (version.templateId != null) "stored" else "default",
            ),
            effective = buildEffective(version, catalog, watchLevel),
            recommendation = buildRecommendation(version, watchLevel),
        )
    }

    fun resolveWatchLevel(riskLevel: RiskLevel, templateId: TemplateId?): WatchLevel {
        return when {
            templateId != null -> try {
                WatchLevel.from(templateId.value)
            } catch (_: Exception) {
                when (riskLevel) {
                    RiskLevel.LOW -> WatchLevel.STANDARD
                    RiskLevel.MEDIUM -> WatchLevel.NIGHT_WANDERING
                    RiskLevel.HIGH -> WatchLevel.FALL_RISK
                }
            }
            else -> when (riskLevel) {
                RiskLevel.LOW -> WatchLevel.STANDARD
                RiskLevel.MEDIUM -> WatchLevel.NIGHT_WANDERING
                RiskLevel.HIGH -> WatchLevel.FALL_RISK
            }
        }
    }

    fun resolveTraits(version: AlarmProfileVersion): List<String> {
        val traits = mutableListOf<String>()
        if (version.riskLevel == RiskLevel.HIGH) traits.add("fall_risk")
        if (version.mobilityAid == MobilityAid.WHEELCHAIR) traits.add("wheelchair_user")
        if (version.mobilityAid == MobilityAid.WALKER) traits.add("walker_user")
        if (version.autopilot) traits.add("autopilot")
        return traits
    }

    // ── Private helpers ──────────────────────────────────────────────

    private fun buildEffective(
        version: AlarmProfileVersion,
        catalog: DagCatalog,
        watchLevel: WatchLevel,
    ): AlarmEffectiveDto {
        val rules = mutableMapOf<String, AlarmRuleDto>()

        catalog.residentStates.forEach { (state, rule) ->
            if (rule.alerts) {
                rules[state.name.lowercase()] = AlarmRuleDto(
                    day = rule.severity.name.lowercase(),
                    night = rule.severity.name.lowercase(),
                    params = mapOf(
                        "warningAfter" to (rule.warningAfter?.toMinutes() ?: 0),
                        "alertAfter" to (rule.alertAfter?.toMinutes() ?: 0),
                        "closure" to rule.closureCondition.name,
                    ),
                )
            }
        }

        return AlarmEffectiveDto(
            level = watchLevel.name.lowercase(),
            mobilityAid = version.mobilityAid?.name?.lowercase() ?: "none",
            mode = version.mode?.name?.lowercase() ?: "preset",
            templateId = version.templateId?.value ?: watchLevel.name.lowercase(),
            rules = rules,
        )
    }

    private fun buildRecommendation(
        version: AlarmProfileVersion,
        currentLevel: WatchLevel,
    ): AlarmRecommendationDto {
        val recommendedLevel = when (version.riskLevel) {
            RiskLevel.LOW -> WatchLevel.STANDARD
            RiskLevel.MEDIUM -> WatchLevel.NIGHT_WANDERING
            RiskLevel.HIGH -> WatchLevel.FALL_RISK
        }

        return AlarmRecommendationDto(
            level = recommendedLevel.name.lowercase(),
            changed = currentLevel != recommendedLevel,
            factors = listOf(
                RiskFactorDto(id = "risk_level", label = "Nivel de riesgo", icon = "warning"),
            ),
            score = when (version.riskLevel) {
                RiskLevel.LOW -> 20
                RiskLevel.MEDIUM -> 50
                RiskLevel.HIGH -> 80
            },
            signalsEvaluated = 3,
            suggestedTemplate = recommendedLevel.name.lowercase(),
            computedAt = version.validFrom.toString(),
        )
    }
}
