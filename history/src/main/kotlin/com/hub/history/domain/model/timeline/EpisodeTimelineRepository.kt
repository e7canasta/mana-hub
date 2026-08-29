package com.hub.history.domain.model.timeline

import com.hub.shared.domain.ResidentId

interface EpisodeTimelineRepository {
    fun findByEpisodeId(episodeId: String, offset: Int = 0, limit: Int = 100): List<EpisodeTimelineEvent>
    fun findByResidentId(residentId: ResidentId, offset: Int = 0, limit: Int = 100): List<EpisodeTimelineEvent>
    fun save(event: EpisodeTimelineEvent): EpisodeTimelineEvent
}
