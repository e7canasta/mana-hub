package com.hub.surveillance.domain.model

import java.util.UUID

@JvmInline
value class EpisodeId(val value: String) {
    companion object {
        fun from(value: String): EpisodeId = EpisodeId(value)
        fun random(): EpisodeId = EpisodeId(UUID.randomUUID().toString())
    }
}
