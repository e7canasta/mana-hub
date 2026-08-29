package com.hub.history.domain.model.timeline

import com.hub.shared.domain.ResidentId

/**
 * Línea de tiempo de un episodio particular.
 *
 * No existe sin episodio. Es el registro cronológico de lo que pasó.
 */
data class EpisodeTimeline(
    val episodeId: String,
    val residentId: ResidentId,
    val events: List<EpisodeTimelineEvent>,
) {
    fun sortedByTime(): EpisodeTimeline =
        copy(events = events.sortedBy { it.at })
}
