package com.hub.surveillance.domain.model

enum class EpisodeStatus {
    PENDING, ACKNOWLEDGED, RESOLVED;

    companion object {
        fun from(value: String): EpisodeStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown episode status: $value")
    }
}
