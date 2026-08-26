package com.hub.surveillance.domain.model

enum class EpisodeSeverity {
    INFO,
    WARNING,
    CRITICAL,
    EMERGENCY;

    companion object {
        fun from(value: String): EpisodeSeverity = when (value.lowercase()) {
            "info" -> INFO
            "warning" -> WARNING
            "critical" -> CRITICAL
            "emergency" -> EMERGENCY
            else -> throw IllegalArgumentException("Unknown episode severity: $value")
        }
    }
}
