package com.hub.observation.domain.model

enum class SceneEventType {
    MOTION_DETECTED, MOTION_STOPPED, PRESENCE_DETECTED, PRESENCE_ENDED,
    STATE_CHANGED, ZONE_ENTER, ZONE_EXIT;

    companion object {
        fun from(value: String): SceneEventType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: value.uppercase().replace("-", "_").let { name ->
                    entries.firstOrNull { it.name == name }
                        ?: throw IllegalArgumentException("Unknown scene event type: $value")
                }
    }
}
