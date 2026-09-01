package com.hub.policy.application.service

import com.hub.policy.application.dto.*
import com.hub.policy.domain.model.WatchLevel
import org.springframework.stereotype.Component

@Component
class AlarmCatalogService {

    fun buildCatalogGroups(): List<AlarmGroupDto> = listOf(
        AlarmGroupDto(id = "bed_exit", label = "Salida de cama", detail = "Alertas por salir de la cama"),
        AlarmGroupDto(id = "wandering", label = "Deambulación", detail = "Alertas por deambular"),
        AlarmGroupDto(id = "bathroom", label = "Baño", detail = "Alertas por uso de baño"),
        AlarmGroupDto(id = "absence", label = "Ausencia", detail = "Alertas por ausencia en la habitación"),
    )

    fun buildCatalogTransitions(): List<AlarmTransitionDto> = listOf(
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

    fun buildCatalogPresets(): Map<String, Map<String, AlarmPresetRuleDto>> = mapOf(
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

    fun buildCatalogTemplates(): List<AlarmTemplateDto> = com.hub.policy.domain.model.DagCatalogs.BY_LEVEL.map { (level, _) ->
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

    fun buildRiskFactors(): List<RiskFactorDto> = listOf(
        RiskFactorDto(id = "falls", label = "Caídas recientes", icon = "warning"),
        RiskFactorDto(id = "night_mobility", label = "Movilidad nocturna", icon = "moon"),
        RiskFactorDto(id = "bathroom_frequency", label = "Frecuencia de baño", icon = "bathroom"),
        RiskFactorDto(id = "absence_duration", label = "Duración de ausencia", icon = "clock"),
        RiskFactorDto(id = "response_time", label = "Tiempo de respuesta del staff", icon = "timer"),
    )
}
