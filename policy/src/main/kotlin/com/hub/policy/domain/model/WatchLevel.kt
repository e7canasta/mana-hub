package com.hub.policy.domain.model

/**
 * Watch levels inspired by mana-hive's DagCatalog.
 * Each level maps to a set of monitoring rules (dwell thresholds, transitions, alerts).
 */
enum class WatchLevel {
    STANDARD,        // Nivel 0: solo observación, sin alertas
    NIGHT_WANDERING, // Nivel 1: alertas básicas nocturnas
    FALL_RISK,       // Nivel 2: alertas intensivas para riesgo de caída
    CRITICAL;        // Nivel 3: alerta inmediata en cualquier movimiento

    companion object {
        fun from(value: String): WatchLevel = when (value.lowercase()) {
            "standard", "low" -> STANDARD
            "night_wandering", "medium" -> NIGHT_WANDERING
            "fall_risk", "high" -> FALL_RISK
            "critical" -> CRITICAL
            else -> throw IllegalArgumentException("Unknown watch level: $value")
        }
    }
}
