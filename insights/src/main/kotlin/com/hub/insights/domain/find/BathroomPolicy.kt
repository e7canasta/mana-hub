package com.hub.insights.domain.find

/**
 * Umbrales de evaluación de baño — value object de negocio.
 *
 *  - bathroomNightEnabled (true): si está apagado, no se evalúa
 *    el aumento de visitas nocturnas. Útil si el residente tiene
 *    patrón conocido de nocturia.
 *  - nightMinAvg (1.0): más de 1 visita nocturna promedio en la última
 *    semana indica riesgo.
 *  - nightRiseFactor (1.5): aumento del 50% entre semanas es tendencia.
 *
 * Ventana nocturna: 22:00–06:00 (definida en BathroomRollup).
 */
data class BathroomPolicy(
    val bathroomNightEnabled: Boolean = true,
    val nightMinAvg: Double = 1.0,
    val nightRiseFactor: Double = 1.5,
)
