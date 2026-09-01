package com.hub.insights.api.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Campos a actualizar. Solo los enviados se modifican; los demás conservan su valor")
data class FindingPolicyUpdateRequest(
    @field:Schema(description = "Umbrales de evaluación de sueño")
    val sleep: SleepPolicyDto? = null,
    @field:Schema(description = "Umbrales de evaluación de cuidado")
    val care: CarePolicyDto? = null,
    @field:Schema(description = "Umbrales de evaluación de visitas nocturnas")
    val bathroom: BathroomPolicyDto? = null,
)

@Schema(description = "Política de evaluación de sueño")
data class SleepPolicyDto(
    @field:Schema(description = "Habilitar regla de sueño inquieto alto", defaultValue = "true", example = "true")
    val restlessHighEnabled: Boolean? = null,
    @field:Schema(description = "Umbral de sueño inquieto (0–1). Por encima se genera hallazgo", defaultValue = "0.25", example = "0.25")
    val restlessHighThreshold: Double? = null,
    @field:Schema(description = "Habilitar regla de sueño fragmentado", defaultValue = "true", example = "true")
    val restlessFragmentedEnabled: Boolean? = null,
    @field:Schema(description = "Umbral de fragmentación (0–1). Por encima es patológico", defaultValue = "0.35", example = "0.35")
    val restlessFragmentedThreshold: Double? = null,
    @field:Schema(description = "Habilitar regla de salidas de cama en aumento", defaultValue = "true", example = "true")
    val exitsRisingEnabled: Boolean? = null,
    @field:Schema(description = "Factor de aumento entre semanas para detectar tendencia", defaultValue = "1.15", example = "1.15")
    val exitsRisingFactor: Double? = null,
    @field:Schema(description = "Diferencia mínima absoluta de salidas entre semanas", defaultValue = "0.3", example = "0.3")
    val exitsRisingMinDelta: Double? = null,
    @field:Schema(description = "Habilitar regla de sueño en rango habitual", defaultValue = "true", example = "true")
    val sleepInRangeEnabled: Boolean? = null,
    @field:Schema(description = "Umbral de variación para considerar dentro del rango", defaultValue = "0.20", example = "0.20")
    val sleepInRangeThreshold: Double? = null,
    @field:Schema(description = "Habilitar regla de baja de sueño semana a semana", defaultValue = "true", example = "true")
    val dropWoWEnabled: Boolean? = null,
    @field:Schema(description = "Baja mínima en minutos de sueño calm entre semanas", defaultValue = "45", example = "45")
    val dropWoWMinutes: Int? = null,
    @field:Schema(description = "Habilitar regla de cluster de alba", defaultValue = "true", example = "true")
    val dawnClusterEnabled: Boolean? = null,
    @field:Schema(description = "Inicio de ventana de alba (HH:mm)", defaultValue = "05:00", example = "05:00")
    val dawnFrom: String? = null,
    @field:Schema(description = "Fin de ventana de alba (HH:mm)", defaultValue = "06:05", example = "06:05")
    val dawnTo: String? = null,
    @field:Schema(description = "Mínimo de salidas en ventana para considerar cluster", defaultValue = "3", example = "3")
    val dawnMinCount: Int? = null,
    @field:Schema(description = "Proporción mínima de salidas en ventana (0–1)", defaultValue = "0.66", example = "0.66")
    val dawnRatio: Double? = null,
)

@Schema(description = "Política de evaluación de cuidado")
data class CarePolicyDto(
    @field:Schema(description = "Habilitar regla de poco cuidado medido. Apagar si la cámara no cubre la habitación", defaultValue = "true", example = "true")
    val careThinEnabled: Boolean? = null,
    @field:Schema(description = "Minutos mínimos diarios de cuidado medido. Por debajo se genera hallazgo", defaultValue = "20.0", example = "20.0")
    val careThinMinutes: Double? = null,
)

@Schema(description = "Política de evaluación de visitas nocturnas al baño")
data class BathroomPolicyDto(
    @field:Schema(description = "Habilitar regla de visitas nocturnas en aumento. Apagar si el residente tiene nocturia conocida", defaultValue = "true", example = "true")
    val bathroomNightEnabled: Boolean? = null,
    @field:Schema(description = "Promedio mínimo de visitas nocturnas por noche para considerar riesgo", defaultValue = "1.0", example = "1.0")
    val nightMinAvg: Double? = null,
    @field:Schema(description = "Factor de aumento entre semanas para detectar tendencia (1.5 = 50%)", defaultValue = "1.5", example = "1.5")
    val nightRiseFactor: Double? = null,
)
