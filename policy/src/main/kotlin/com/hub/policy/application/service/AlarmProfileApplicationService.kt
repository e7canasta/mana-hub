package com.hub.policy.application.service

import com.hub.policy.application.dto.*
import com.hub.policy.domain.model.*
import com.hub.policy.domain.repository.AlarmProfileRepository
import com.hub.shared.domain.ResidentId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AlarmProfileApplicationService(
    private val alarmProfileRepository: AlarmProfileRepository,
    private val auditService: com.hub.audit.domain.service.AuditService
) {

    @Transactional(readOnly = true)
    fun getResidentProfile(residentId: String): AlarmProfileResponse? {
        val version = alarmProfileRepository.findCurrentByResidentId(ResidentId(residentId))
            ?: return null

        val watchLevel = resolveWatchLevel(version.riskLevel, version.templateId)
        val catalog = DagCatalogs.forLevel(watchLevel)
        val traits = resolveTraits(version)

        return AlarmProfileResponse(
            resident = AlarmProfileResidentDto(
                id = residentId,
                fullName = "",
                traits = traits,
            ),
            profile = AlarmProfileSettingsDto(
                riskLevel = version.riskLevel.name.lowercase(),
                mobilityAid = version.mobilityAid ?: "none",
                autopilot = version.autopilot,
                mode = version.mode ?: "preset",
                templateId = version.templateId ?: watchLevel.name.lowercase(),
                overrides = emptyMap(),
                updatedAt = version.validFrom.toString(),
                updatedBy = version.updatedBy,
                updatedByName = null,
                source = if (version.templateId != null) "stored" else "default",
            ),
            effective = buildEffective(version, catalog, watchLevel),
            recommendation = buildRecommendation(version, watchLevel),
        )
    }

    @Transactional
    fun updateResidentProfile(residentId: String, request: UpdateAlarmProfileRequest): AlarmProfileResponse {
        val rid = ResidentId(residentId)
        alarmProfileRepository.expireCurrentByResidentId(rid)

        val riskLevel = request.riskLevel?.let { RiskLevel.from(it) } ?: RiskLevel.MEDIUM
        val templateId = request.templateId ?: resolveWatchLevel(riskLevel, null).name.lowercase()

        val newProfile = AlarmProfileVersion.create(rid, request.updatedBy).update(
            mobilityAid = request.mobilityAid,
            autopilot = request.autopilot,
            mode = request.mode,
            templateId = templateId,
            overridesJson = request.overridesJson,
            riskLevel = riskLevel,
            updatedBy = request.updatedBy,
        )

        alarmProfileRepository.save(newProfile)

        if (request.reason != null) {
            auditService.recordAction(
                actorId = request.updatedBy ?: "system",
                action = "alarm_profile.update",
                entityType = "AlarmProfileVersion",
                entityId = residentId,
                metadataJson = """{"reason":"${request.reason.replace("\"", "\\\"")}","riskLevel":"${riskLevel.name}"}"""
            )
        }

        return getResidentProfile(residentId)!!
    }

    @Transactional(readOnly = true)
    fun getProfileHistory(residentId: String): List<AlarmProfileResponse> {
        return alarmProfileRepository.findByResidentId(ResidentId(residentId)).map { version ->
            val watchLevel = resolveWatchLevel(version.riskLevel, version.templateId)
            val catalog = DagCatalogs.forLevel(watchLevel)
            AlarmProfileResponse(
                resident = AlarmProfileResidentDto(id = residentId, fullName = "", traits = emptyList()),
                profile = AlarmProfileSettingsDto(
                    riskLevel = version.riskLevel.name.lowercase(),
                    mobilityAid = version.mobilityAid ?: "none",
                    autopilot = version.autopilot,
                    mode = version.mode ?: "preset",
                    templateId = version.templateId ?: watchLevel.name.lowercase(),
                    overrides = emptyMap(),
                    updatedAt = version.validFrom.toString(),
                    updatedBy = version.updatedBy,
                    updatedByName = null,
                    source = if (version.templateId != null) "stored" else "default",
                ),
                effective = buildEffective(version, catalog, watchLevel),
                recommendation = buildRecommendation(version, watchLevel),
            )
        }
    }

    @Transactional(readOnly = true)
    fun getCatalog(): AlarmPresetCatalogResponse {
        return AlarmPresetCatalogResponse(
            levels = listOf("low", "medium", "high"),
            mobilityAids = listOf("none", "walker", "wheelchair"),
            actions = listOf("off", "notify", "alarm"),
            shifts = listOf("day", "night"),
            modes = listOf("preset", "custom"),
            sensitivities = listOf("low", "standard", "high"),
            groups = buildCatalogGroups(),
            transitions = buildCatalogTransitions(),
            presets = buildCatalogPresets(),
            templates = buildCatalogTemplates(),
            riskFactors = buildRiskFactors(),
            autopilot = AutopilotConfigDto(
                minimumSignalsForRaise = 3,
                minimumDaysBetweenChanges = 7,
            ),
        )
    }

    // ── Private helpers ──────────────────────────────────────────────

    private fun resolveWatchLevel(riskLevel: RiskLevel, templateId: String?): WatchLevel {
        return when {
            templateId != null -> try {
                WatchLevel.from(templateId)
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

    private fun resolveTraits(version: AlarmProfileVersion): List<String> {
        val traits = mutableListOf<String>()
        if (version.riskLevel == RiskLevel.HIGH) traits.add("fall_risk")
        if (version.mobilityAid == "wheelchair") traits.add("wheelchair_user")
        if (version.mobilityAid == "walker") traits.add("walker_user")
        if (version.autopilot) traits.add("autopilot")
        return traits
    }

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
            mobilityAid = version.mobilityAid ?: "none",
            mode = version.mode ?: "preset",
            templateId = version.templateId ?: watchLevel.name.lowercase(),
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

    private fun buildCatalogGroups(): List<AlarmGroupDto> = listOf(
        AlarmGroupDto(id = "bed_exit", label = "Salida de cama", detail = "Alertas por salir de la cama"),
        AlarmGroupDto(id = "wandering", label = "Deambulación", detail = "Alertas por deambular"),
        AlarmGroupDto(id = "bathroom", label = "Baño", detail = "Alertas por uso de baño"),
        AlarmGroupDto(id = "absence", label = "Ausencia", detail = "Alertas por ausencia en la habitación"),
    )

    private fun buildCatalogTransitions(): List<AlarmTransitionDto> = listOf(
        AlarmTransitionDto(
            id = "lying_to_sitting", group = "bed_exit", label = "Acostado → Sentado",
            shortLabel = "Sentarse", detail = "Residente se sienta en la cama",
            pictogram = "person_sitting", art = "figure", locked = false,
            requiresAid = null, params = emptyList(),
        ),
        AlarmTransitionDto(
            id = "lying_to_standing", group = "bed_exit", label = "Acostado → De pie",
            shortLabel = "Levantarse", detail = "Residente se levanta de la cama",
            pictogram = "person_standing", art = "figure", locked = false,
            requiresAid = null, params = emptyList(),
        ),
        AlarmTransitionDto(
            id = "standing_to_bathroom", group = "bathroom", label = "De pie → Baño",
            shortLabel = "Baño", detail = "Residente entra al baño",
            pictogram = "door_open", art = "scene", locked = false,
            requiresAid = null, params = emptyList(),
        ),
        AlarmTransitionDto(
            id = "standing_to_absent", group = "absence", label = "De pie → Ausente",
            shortLabel = "Ausente", detail = "Residente sale de la habitación",
            pictogram = "person_leaving", art = "figure", locked = false,
            requiresAid = null, params = emptyList(),
        ),
    )

    private fun buildCatalogPresets(): Map<String, Map<String, AlarmPresetRuleDto>> = mapOf(
        "bed_exit" to mapOf(
            "standard" to AlarmPresetRuleDto(day = "notify", night = "alarm", params = null),
            "fall_risk" to AlarmPresetRuleDto(day = "alarm", night = "alarm", params = null),
        ),
        "wandering" to mapOf(
            "standard" to AlarmPresetRuleDto(day = "off", night = "notify", params = null),
            "fall_risk" to AlarmPresetRuleDto(day = "notify", night = "alarm", params = null),
        ),
        "bathroom" to mapOf(
            "standard" to AlarmPresetRuleDto(day = "off", night = "notify", params = null),
            "fall_risk" to AlarmPresetRuleDto(day = "notify", night = "alarm", params = null),
        ),
    )

    private fun buildCatalogTemplates(): List<AlarmTemplateDto> = DagCatalogs.BY_LEVEL.map { (level, _) ->
        AlarmTemplateDto(
            id = level.name.lowercase(),
            label = when (level) {
                WatchLevel.STANDARD -> "Monitoreo General"
                WatchLevel.NIGHT_WANDERING -> "Vigilia Nocturna"
                WatchLevel.FALL_RISK -> "Riesgo de Caída"
                WatchLevel.CRITICAL -> "Crítico"
            },
            detail = when (level) {
                WatchLevel.STANDARD -> "Solo observación, sin alertas"
                WatchLevel.NIGHT_WANDERING -> "Alertas básicas para horario nocturno"
                WatchLevel.FALL_RISK -> "Alertas intensivas para residentes con riesgo de caída"
                WatchLevel.CRITICAL -> "Alerta inmediata en cualquier movimiento"
            },
            recommendedFor = when (level) {
                WatchLevel.STANDARD -> listOf("low_risk", "independent")
                WatchLevel.NIGHT_WANDERING -> listOf("medium_risk", "nocturnal_wanderer")
                WatchLevel.FALL_RISK -> listOf("high_risk", "fall_history", "walker_user")
                WatchLevel.CRITICAL -> listOf("critical", "wheelchair_user", "recent_fall")
            },
            rules = emptyMap(),
        )
    }

    private fun buildRiskFactors(): List<RiskFactorDto> = listOf(
        RiskFactorDto(id = "falls", label = "Caídas recientes", icon = "warning"),
        RiskFactorDto(id = "night_mobility", label = "Movilidad nocturna", icon = "moon"),
        RiskFactorDto(id = "bathroom_frequency", label = "Frecuencia de baño", icon = "bathroom"),
        RiskFactorDto(id = "absence_duration", label = "Duración de ausencia", icon = "clock"),
        RiskFactorDto(id = "response_time", label = "Tiempo de respuesta del staff", icon = "timer"),
    )
}
