package com.hub.policy.domain.model

enum class PolicyMode {
    PRESET,
    CUSTOM;

    companion object {
        fun from(value: String): PolicyMode = when (value.lowercase()) {
            "preset", "" -> PRESET
            "custom" -> CUSTOM
            else -> throw IllegalArgumentException("Unknown policy mode: $value")
        }
    }
}
