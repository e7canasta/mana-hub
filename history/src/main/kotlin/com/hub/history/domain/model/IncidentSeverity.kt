package com.hub.history.domain.model

enum class IncidentSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    companion object {
        fun from(value: String): IncidentSeverity = when (value.lowercase()) {
            "low" -> LOW
            "medium" -> MEDIUM
            "high" -> HIGH
            "critical" -> CRITICAL
            else -> throw IllegalArgumentException("Unknown severity: $value")
        }
    }
}
