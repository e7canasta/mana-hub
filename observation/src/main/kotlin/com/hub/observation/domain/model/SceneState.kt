package com.hub.observation.domain.model

enum class SceneState {
    EMPTY, OCCUPIED, SLEEPING, WANDERING, BATHROOM, UNKNOWN;

    companion object {
        fun from(value: String): SceneState =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: value.uppercase().replace("-", "_").let { name ->
                    entries.firstOrNull { it.name == name } ?: UNKNOWN
                }
    }
}
