package com.hub.history.domain.model

import java.util.UUID

@JvmInline
value class HistoryEpisodeId(val value: String) {
    companion object {
        fun from(value: String): HistoryEpisodeId = HistoryEpisodeId(value)
        fun random(): HistoryEpisodeId = HistoryEpisodeId(UUID.randomUUID().toString())
    }
}
