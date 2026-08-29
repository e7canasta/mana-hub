package com.hub.history.domain.model.timeline

import com.hub.shared.domain.ResidentId
import java.time.Instant

/**
 * Evento puntual en la línea de tiempo de un episodio.
 *
 * Nombre específico: EpisodeTimelineEvent (no "Event" genérico).
 * Siempre asociado a un episodio particular.
 */
data class EpisodeTimelineEvent(
    val id: EpisodeTimelineEventId,
    val episodeId: String,
    val residentId: ResidentId,
    val at: Instant,
    val type: EventType,
    val fromState: String?,
    val toState: String?,
    val description: String?,
)
