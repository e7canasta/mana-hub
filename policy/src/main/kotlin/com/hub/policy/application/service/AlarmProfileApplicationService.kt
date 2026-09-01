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
    private val eventPublisher: DomainEventPublisher,
    private val responseBuilder: AlarmProfileResponseBuilder,
    private val catalogService: AlarmCatalogService,
    private val objectMapper: com.fasterxml.jackson.databind.ObjectMapper,
) {

    @Transactional(readOnly = true)
    fun getResidentProfile(residentId: String): AlarmProfileResponse? {
        val version = alarmProfileRepository.findCurrentByResidentId(ResidentId(residentId))
            ?: return null

        val watchLevel = responseBuilder.resolveWatchLevel(version.riskLevel, version.templateId)
        val catalog = DagCatalogs.forLevel(watchLevel)
        val traits = responseBuilder.resolveTraits(version)

        val typedOverrides = alarmProfileOverrideRepository.findByProfileVersionId(version.id.value)
        val overridesMap = typedOverridesToMap(typedOverrides)

        return responseBuilder.buildResponse(
            version = version,
            residentId = residentId,
            watchLevel = watchLevel,
            catalog = catalog,
            overridesMap = overridesMap,
            traits = traits,
        )
    }

    @Transactional
    fun updateResidentProfile(residentId: String, request: UpdateAlarmProfileRequest): AlarmProfileResponse {
        val rid = ResidentId(residentId)

        /*
         * La version nueva parte de la que estaba vigente, no de cero.
         *
         * Antes se hacia AlarmProfileVersion.create(...) y se le aplicaba solo
         * lo que venia en el request, asi que todo campo omitido volvia a su
         * default: mandar unicamente {"autopilot": true} reseteaba el riskLevel
         * a MEDIUM, el mobilityAid a NONE y el mode a preset.
         *
         * Eso hace que el verbo mienta. PATCH es una actualizacion parcial -el
         * cliente manda lo que cambia- y el que escribe la pantalla hace
         * exactamente eso, y sin querer borra los otros dos campos. Con la
         * version actual como base, omitir un campo significa "dejalo como
         * esta", que es lo que PATCH promete.
         */
        val current = alarmProfileRepository.findCurrentByResidentId(rid)
        alarmProfileRepository.expireCurrentByResidentId(rid)

        val riskLevel = request.riskLevel?.let { RiskLevel.from(it) }
            ?: current?.riskLevel
            ?: RiskLevel.MEDIUM
        val templateId = request.templateId?.let { TemplateId.from(it) }
            ?: current?.templateId
            ?: TemplateId.from(responseBuilder.resolveWatchLevel(riskLevel, null).name.lowercase())

        val newProfile = AlarmProfileVersion.create(rid, request.updatedBy).update(
            mobilityAid = request.mobilityAid?.let { MobilityAid.from(it) } ?: current?.mobilityAid,
            autopilot = request.autopilot ?: current?.autopilot,
            mode = request.mode?.let { PolicyMode.from(it) } ?: current?.mode,
            templateId = templateId,
            riskLevel = riskLevel,
            updatedBy = request.updatedBy,
        )

        alarmProfileRepository.save(newProfile)
        publishAggregateEvents(newProfile)

        /* Los overrides siguen la misma regla: si el request no los menciona, se
         * conservan los de la version anterior. Omitir no es borrar. */
        if (request.overridesJson == null && current != null) {
            val carried = alarmProfileOverrideRepository.findByProfileVersionId(current.id.value)
            if (carried.isNotEmpty()) {
                alarmProfileOverrideRepository.saveAll(carried, newProfile.id.value)
            }
        }

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
            val watchLevel = responseBuilder.resolveWatchLevel(version.riskLevel, version.templateId)
            val catalog = DagCatalogs.forLevel(watchLevel)
            val typedOverrides = alarmProfileOverrideRepository.findByProfileVersionId(version.id.value)
            val overridesMap = typedOverridesToMap(typedOverrides)
            responseBuilder.buildResponse(
                version = version,
                residentId = residentId,
                watchLevel = watchLevel,
                catalog = catalog,
                overridesMap = overridesMap,
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
            groups = catalogService.buildCatalogGroups(),
            transitions = catalogService.buildCatalogTransitions(),
            presets = catalogService.buildCatalogPresets(),
            templates = catalogService.buildCatalogTemplates(),
            riskFactors = catalogService.buildRiskFactors(),
            autopilot = AutopilotConfigDto(
                minimumSignalsForRaise = 3,
                minimumDaysBetweenChanges = 7,
            ),
        )
    }

    // ── Private helpers ──────────────────────────────────────────────

    private fun parseOverridesJson(json: String): Map<String, Any> {
        if (json.isBlank() || json == "{}") return emptyMap()
        return try {
            @Suppress("UNCHECKED_CAST")
            objectMapper.readValue(json, Map::class.java) as Map<String, Any>
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
            val sev = o.severity
            val clo = o.closureCondition
            val obs = o.observeOnly
            if (sev != null) entry["severity"] = sev
            if (clo != null) entry["closureCondition"] = clo
            if (obs != null) entry["observeOnly"] = obs

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
                        severity = raw["severity"] as? String,
                        closureCondition = raw["closureCondition"] as? String,
                        observeOnly = raw["observeOnly"] as? Boolean,
                    )
                )
                "dwell" -> result.add(
                    PolicyOverride.DwellOverride(
                        id = id,
                        ruleId = ruleId,
                        stateKind = raw["stateKind"] as? String ?: ruleId,
                        warningAfterMinutes = (raw["warningAfterMinutes"] as? Number)?.toInt(),
                        alertAfterMinutes = (raw["alertAfterMinutes"] as? Number)?.toInt(),
                        severity = raw["severity"] as? String,
                        closureCondition = raw["closureCondition"] as? String,
                        observeOnly = raw["observeOnly"] as? Boolean,
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
                        observeOnly = raw["observeOnly"] as? Boolean,
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

    private fun publishAggregateEvents(profile: AlarmProfileVersion) {
        profile.domainEvents.forEach { eventPublisher.publish(it) }
        profile.clearEvents()
    }
}
