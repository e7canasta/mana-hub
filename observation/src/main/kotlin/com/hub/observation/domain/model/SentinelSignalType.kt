package com.hub.observation.domain.model

enum class SentinelSignalType {
    FALL_RISK, WANDER_RISK, SLEEP_DISRUPTION, BATHROOM_URGENCY,
    ABSENCE, PROLONGED_STAY, RAPID_MOVEMENT, NO_MOVEMENT;

    companion object {
        fun from(value: String): SentinelSignalType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: value.uppercase().replace("-", "_").let { name ->
                    entries.firstOrNull { it.name == name } ?: NO_MOVEMENT
                }
    }
}
