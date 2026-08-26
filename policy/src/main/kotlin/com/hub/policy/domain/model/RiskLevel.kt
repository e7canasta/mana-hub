package com.hub.policy.domain.model

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH;

    companion object {
        fun from(value: String): RiskLevel = when (value.lowercase()) {
            "low" -> LOW
            "medium" -> MEDIUM
            "high" -> HIGH
            else -> throw IllegalArgumentException("Unknown risk level: $value")
        }
    }
}