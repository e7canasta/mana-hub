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
) {

    @Transactional(readOnly = true)
    fun getTimelineByEpisode(episodeId: String, offset: Int = 0, limit: Int = 100): EpisodeTimeline? {
        val events = repository.findByEpisodeId(episodeId, offset, limit)
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
