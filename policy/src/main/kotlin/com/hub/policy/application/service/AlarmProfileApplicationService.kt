package com.hub.policy.application.service

import com.hub.policy.application.dto.*
import com.hub.policy.domain.model.*
import com.hub.policy.domain.repository.AlarmProfileOverrideRepository
import com.hub.policy.domain.repository.AlarmProfileRepository
import com.hub.shared.domain.DomainEvent
import com.hub.shared.domain.DomainEventPublisher
import com.hub.shared.domain.ResidentId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

data class AlarmProfileChangedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val occurredAt: Instant = Instant.now(),
    override val eventType: String = "AlarmProfileChanged",
    val residentId: String,
    val versionId: String,
    val riskLevel: String,
    val templateId: String?
) : DomainEvent

@Service
class AlarmProfileApplicationService(
    private val alarmProfileRepository: AlarmProfileRepository,
    private val alarmProfileOverrideRepository: AlarmProfileOverrideRepository,
    private val auditService: com.hub.audit.domain.service.AuditService,
    private val eventPublisher: DomainEventPublisher
) {

    @Transactional(readOnly = true)
    fun getResidentProfile(residentId: String): AlarmProfileResponse? {
        val version = alarmProfileRepository.findCurrentByResidentId(ResidentId(residentId))
            ?: return null

        val watchLevel = resolveWatchLevel(version.riskLevel, version.templateId)
        val catalog = DagCatalogs.forLevel(watchLevel)
        val traits = resolveTraits(version)

        val typedOverrides = alarmProfileOverrideRepository.findByProfileVersionId(version.id.value)
        val overridesMap = typedOverridesToMap(typedOverrides)

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

    @Transactional
    fun updateResidentProfile(residentId: String, request: UpdateAlarmProfileRequest): AlarmProfileResponse {
        val rid = ResidentId(residentId)
        alarmProfileRepository.expireCurrentByResidentId(rid)

        val riskLevel = request.riskLevel?.let { RiskLevel.from(it) } ?: RiskLevel.MEDIUM
        val templateId = request.templateId?.let { TemplateId.from(it) }
            ?: TemplateId.from(resolveWatchLevel(riskLevel, null).name.lowercase())

        val newProfile = AlarmProfileVersion.create(rid, request.updatedBy).update(
            mobilityAid = request.mobilityAid?.let { MobilityAid.from(it) },
            autopilot = request.autopilot,
            mode = request.mode?.let { PolicyMode.from(it) },
            templateId = templateId,
            riskLevel = riskLevel,
            updatedBy = request.updatedBy,
        )

        alarmProfileRepository.save(newProfile)

        if (request.overridesJson != null && request.overridesJson != "{}") {
            val overridesFromRequest = parseOverridesJson(request.overridesJson)
            if (overridesFromRequest.isNotEmpty()) {
                val typedOverrides = overridesMapToTyped(overridesFromRequest)
                if (typedOverrides.isNotEmpty()) {
                    alarmProfileOverrideRepository.saveAll(typedOverrides, newProfile.id.value)
                }
            }
        }

        if (request.reason != null) {
            auditService.recordAction(
                actorId = request.updatedBy ?: "system",
                action = "alarm_profile.update",
                entityType = "AlarmProfileVersion",
                entityId = residentId,
                metadataJson = """{"reason":"${request.reason.replace("\"", "\\\"")}","riskLevel":"${riskLevel.name}"}"""
            )
        }

        // Publish domain event for hub→hive bridge (outbox pattern)
        eventPublisher.publish(
            AlarmProfileChangedEvent(
                residentId = residentId,
                versionId = newProfile.id.value,
                riskLevel = riskLevel.name,
                templateId = templateId?.value
            )
        )

        return getResidentProfile(residentId)!!
    }

    @Transactional(readOnly = true)
    fun getProfileHistory(residentId: String): List<AlarmProfileResponse> {
        return alarmProfileRepository.findByResidentId(ResidentId(residentId)).map { version ->
            val watchLevel = resolveWatchLevel(version.riskLevel, version.templateId)
            val catalog = DagCatalogs.forLevel(watchLevel)
            val typedOverrides = alarmProfileOverrideRepository.findByProfileVersionId(version.id.value)
            val overridesMap = typedOverridesToMap(typedOverrides)
            AlarmProfileResponse(
                resident = AlarmProfileResidentDto(id = residentId, fullName = "", traits = emptyList()),
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

    private fun resolveWatchLevel(riskLevel: RiskLevel, templateId: TemplateId?): WatchLevel {
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

    private fun resolveTraits(version: AlarmProfileVersion): List<String> {
        val traits = mutableListOf<String>()
        if (version.riskLevel == RiskLevel.HIGH) traits.add("fall_risk")
        if (version.mobilityAid == MobilityAid.WHEELCHAIR) traits.add("wheelchair_user")
        if (version.mobilityAid == MobilityAid.WALKER) traits.add("walker_user")
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

    private fun parseOverridesJson(json: String): Map<String, Any> {
        if (json.isBlank() || json == "{}") return emptyMap()
        return try {
            val mapper = com.fasterxml.jackson.databind.ObjectMapper()
            @Suppress("UNCHECKED_CAST")
            mapper.readValue(json, Map::class.java) as Map<String, Any>
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun typedOverridesToMap(overrides: List<PolicyOverride>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        for (o in overrides) {
            val entry = mutableMapOf<String, Any>()
            when (o) {
                is PolicyOverride.HysteresisOverride -> {
                    entry["type"] = "hysteresis"
                    entry["transitionKey"] = o.transitionKey
                    entry["hysteresisSeconds"] = o.hysteresisSeconds
                }
                is PolicyOverride.DwellOverride -> {
                    entry["type"] = "dwell"
                    entry["stateKind"] = o.stateKind
                    if (o.warningAfterMinutes != null) entry["warningAfterMinutes"] = o.warningAfterMinutes
                    if (o.alertAfterMinutes != null) entry["alertAfterMinutes"] = o.alertAfterMinutes
                }
                is PolicyOverride.ComeBackOverride -> {
                    entry["type"] = "comeback"
                    entry["baselineState"] = o.baselineState
                    if (o.warningAfterMinutes != null) entry["warningAfterMinutes"] = o.warningAfterMinutes
                    if (o.alertAfterMinutes != null) entry["alertAfterMinutes"] = o.alertAfterMinutes
                    if (o.severity != null) entry["severity"] = o.severity
                    if (o.closureCondition != null) entry["closureCondition"] = o.closureCondition
                }
            }
            result[o.ruleId] = entry
        }
        return result
    }

    private fun overridesMapToTyped(map: Map<String, Any>): List<PolicyOverride> {
        val result = mutableListOf<PolicyOverride>()
        for ((ruleId, raw) in map) {
            if (raw !is Map<*, *>) continue
            val id = com.hub.shared.domain.Identifier()
            val explicitType = raw["type"] as? String
            val inferredType = explicitType ?: inferOverrideType(raw)
            when (inferredType) {
                "hysteresis" -> result.add(
                    PolicyOverride.HysteresisOverride(
                        id = id,
                        ruleId = ruleId,
                        transitionKey = raw["transitionKey"] as? String ?: "",
                        hysteresisSeconds = (raw["hysteresisSeconds"] as? Number)?.toInt() ?: 0,
                    )
                )
                "dwell" -> result.add(
                    PolicyOverride.DwellOverride(
                        id = id,
                        ruleId = ruleId,
                        stateKind = raw["stateKind"] as? String ?: ruleId,
                        warningAfterMinutes = (raw["warningAfterMinutes"] as? Number)?.toInt(),
                        alertAfterMinutes = (raw["alertAfterMinutes"] as? Number)?.toInt(),
                    )
                )
                "comeback" -> result.add(
                    PolicyOverride.ComeBackOverride(
                        id = id,
                        ruleId = ruleId,
                        baselineState = raw["baselineState"] as? String ?: ruleId,
                        warningAfterMinutes = (raw["warningAfterMinutes"] as? Number)?.toInt(),
                        alertAfterMinutes = (raw["alertAfterMinutes"] as? Number)?.toInt(),
                        severity = raw["severity"] as? String,
                        closureCondition = raw["closureCondition"] as? String,
                    )
                )
            }
        }
        return result
    }

    private fun inferOverrideType(raw: Map<*, *>): String {
        if (raw.containsKey("hysteresisSeconds") || raw.containsKey("transitionKey")) return "hysteresis"
        if (raw.containsKey("baselineState") || raw.containsKey("severity") || raw.containsKey("closureCondition")) return "comeback"
        if (raw.containsKey("warningAfterMinutes") || raw.containsKey("alertAfterMinutes")) return "dwell"
        return "dwell"
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
