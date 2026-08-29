package com.hub.history.application.dto

data class EpisodeTimelineDto(
    val episodeId: String,
    val residentId: String,
    val events: List<EpisodeTimelineEventDto>,
)

data class EpisodeTimelineEventDto(
    val id: String,
    val at: String,
    val type: String,
    val fromState: String?,
    val toState: String?,
    val description: String?,
)
