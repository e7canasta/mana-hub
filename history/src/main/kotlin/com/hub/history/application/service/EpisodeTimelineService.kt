package com.hub.history.application.service

import com.hub.history.domain.model.timeline.EpisodeTimeline
import com.hub.history.domain.model.timeline.EpisodeTimelineEvent
import com.hub.history.domain.model.timeline.EpisodeTimelineRepository
import com.hub.shared.domain.ResidentId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EpisodeTimelineService(
    private val repository: EpisodeTimelineRepository,
    private val deriver: EpisodeTimelineDeriver,
) {

    /**
     * Lo guardado gana; si no hay nada, se deriva de las señales y la escena.
     *
     * La regla vive acá y en un solo lugar a propósito. `EpisodeTimelineBuilder`
     * escribe cuando el evento entra por el webhook del bridge, pero los
     * episodios que produce el motor entran directo a `sentinel_signals` y
     * `scene_events`, así que no tienen filas y esto devolvía `null` — un 404
     * para justamente los episodios que el panel necesita mostrar.
     *
     * Sigue devolviendo `null` cuando no hay **ninguna** de las dos fuentes: ahí
     * el 404 es verdad, el episodio no existe o no dejó rastro.
     */
    @Transactional(readOnly = true)
    fun getTimelineByEpisode(episodeId: String, offset: Int = 0, limit: Int = 100): EpisodeTimeline? {
        val stored = repository.findByEpisodeId(episodeId, offset, limit)
        val events = stored.ifEmpty {
            if (offset > 0) emptyList() else deriver.derive(episodeId)
        }
        if (events.isEmpty() && offset == 0) return null
        return EpisodeTimeline(
            episodeId = episodeId,
            residentId = events.firstOrNull()?.residentId ?: ResidentId("unknown"),
            events = events,
        ).sortedByTime()
    }

    @Transactional(readOnly = true)
    fun getEventsByResident(residentId: String, offset: Int = 0, limit: Int = 100): List<EpisodeTimelineEvent> {
        return repository.findByResidentId(ResidentId(residentId), offset, limit)
    }
}
