package com.hub.history.domain.model.timeline

@JvmInline
value class EpisodeTimelineEventId(val value: String) {
    companion object {
        fun random() = EpisodeTimelineEventId(java.util.UUID.randomUUID().toString())
        fun from(raw: String) = EpisodeTimelineEventId(raw)
    }
}
