package com.hub.insights.domain.find

/**
 * Umbrales de evaluación de sueño — value object de negocio.
 *
 * Cada regla tiene un flag `enabled` y su umbral correspondiente.
 * El director médico puede prender/apagar cada regla individualmente
 * por residente, y ajustar los umbrales.
 *
 * Los defaults vienen de evidencia clínica:
 *
 *  - restlessHighThreshold (0.25): Pittsburgh Sleep Quality Index —
 *    sueño inquieto > 25% del total dormido indica fragmentación.
 *  - restlessFragmentedThreshold (0.35): por encima de 35% es patológico,
 *    requiere revisión de perfil ComeBack y rondas de madrugada.
 *  - exitsRisingFactor (1.15): aumento del 15% entre semanas es
 *    significativo clínicamente (no ruido estadístico).
 *  - exitsRisingMinDelta (0.3): mínimo 0.3 salidas de diferencia para
 *    que el porcentaje no sea artefacto de baja frecuencia.
 *  - sleepInRangeThreshold (0.20): dentro del 20% = rango habitual
 *    del residente, sin intervención necesaria.
 *  - dropWoWMinutes (45): baja de 45 min calm entre semanas es un
 *    cambio real, no variación normal.
 *  - dawnFrom/dawnTo (05:00–06:05): ventana de alba según ritmo
 *    circadiano — momento de mayor riesgo de caída.
 *  - dawnMinCount (3): mínimo 3 salidas para considerar patrón,
 *    no un evento aislado.
 *  - dawnRatio (0.66): 2/3 de las salidas en ventana = cluster
 *    confirmado estadísticamente.
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
