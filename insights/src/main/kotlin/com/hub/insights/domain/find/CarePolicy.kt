package com.hub.insights.domain.find

/**
 * Umbrales de evaluación de cuidado — value object de negocio.
 *
 *  - careThinEnabled (true): si está apagado, no se evalúa si hay
 *    poco cuidado medido. Útil si la cámara no cubre la habitación.
 *  - careThinMinutes (20.0): menos de 20 min/día de cuidado medido
 *    indica baja interacción observable por cámara.
 */
data class CarePolicy(
    val careThinEnabled: Boolean = true,
    val careThinMinutes: Double = 20.0,
)
