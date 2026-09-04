package com.hub.observation.domain.model

enum class SceneState {
    EMPTY, OCCUPIED, SLEEPING, WANDERING, BATHROOM,
    LYING, SITTING_IN_BED, ATTEMPTING_EXIT, BED_EDGE,
    STANDING, ON_FLOOR, IN_BATHROOM, IN_ROOM, IN_HALLWAY,
    OUTDOOR, ABSENT, IN_CHAIR, IN_WHEELCHAIR, UNKNOWN;

    companion object {
        fun from(value: String): SceneState =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: value.trim()
                    .replace(Regex("([a-z])([A-Z])"), "$1_$2")
                    .uppercase()
                    .replace("-", "_")
                    .let { name ->
                    entries.firstOrNull { it.name == name } ?: UNKNOWN
                }
    }
}
