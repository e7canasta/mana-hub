package com.hub.insights.domain.find

/**
 * Umbrales de evaluación de sueño — value object de negocio.
 *
 * Cada regla tiene un flag `enabled` y su umbral correspondiente.
 * El director médico puede prender/apagar cada regla individualmente
 * por residente, y ajustar los umbrales.
 */
data class SleepPolicy(
    val restlessHighEnabled: Boolean = true,
    val restlessHighThreshold: Double = 0.25,
    val restlessFragmentedEnabled: Boolean = true,
    val restlessFragmentedThreshold: Double = 0.35,
    val exitsRisingEnabled: Boolean = true,
    val exitsRisingFactor: Double = 1.15,
    val exitsRisingMinDelta: Double = 0.3,
    val sleepInRangeEnabled: Boolean = true,
    val sleepInRangeThreshold: Double = 0.20,
    val dropWoWEnabled: Boolean = true,
    val dropWoWMinutes: Int = 45,
    val dawnClusterEnabled: Boolean = true,
    val dawnFrom: String = "05:00",
    val dawnTo: String = "06:05",
    val dawnMinCount: Int = 3,
    val dawnRatio: Double = 0.66,
)
