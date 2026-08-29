package com.hub.history.domain.model

enum class EpisodeKind {
    FALL,
    BED_EXIT,
    WANDERING,
    BATHROOM,
    ABSENCE,
    OTHER;

    companion object {
        fun from(value: String): EpisodeKind = when (value.uppercase()) {
            "FALL" -> FALL
            "BED_EXIT" -> BED_EXIT
            "WANDERING" -> WANDERING
            "BATHROOM" -> BATHROOM
            "ABSENCE" -> ABSENCE
            else -> OTHER
        }
    }
}
