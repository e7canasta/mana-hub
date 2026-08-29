package com.hub.surveillance.domain.repository

import com.hub.surveillance.domain.model.Episode
import com.hub.surveillance.domain.model.EpisodeId
import com.hub.shared.domain.ResidentId

interface EpisodeRepository {
    fun findById(id: EpisodeId): Episode?
    fun findByResidentId(residentId: ResidentId): List<Episode>
    fun findPending(): List<Episode>
    fun save(episode: Episode): Episode
}
