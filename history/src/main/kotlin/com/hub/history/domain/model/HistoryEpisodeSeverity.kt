package com.hub.history.domain.model

enum class HistoryEpisodeSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    companion object {
        fun from(value: String): HistoryEpisodeSeverity = when (value.lowercase()) {
            "low" -> LOW
            "medium" -> MEDIUM
            "high" -> HIGH
            "critical" -> CRITICAL
            else -> throw IllegalArgumentException("Unknown severity: $value")
        }
    }
}
