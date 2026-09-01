package com.hub.observation.domain.model

enum class TriggerType {
    SCHEDULED, EVENT_DRIVEN, MANUAL, THRESHOLD, ANOMALY;

    companion object {
        fun from(value: String): TriggerType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: value.uppercase().replace("-", "_").let { name ->
                    entries.firstOrNull { it.name == name } ?: SCHEDULED
                }
    }
}
