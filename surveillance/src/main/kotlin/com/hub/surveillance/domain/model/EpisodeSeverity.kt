package com.hub.surveillance.domain.model

enum class EpisodeSeverity {
    INFO,
    WARNING,
    CRITICAL,
    EMERGENCY;

    /** Orden para comparar severidad: mayor ordinal = mayor gravedad */
    fun isMoreSevereThan(other: EpisodeSeverity): Boolean = this.ordinal > other.ordinal

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
