package com.hub.shared.domain.port

import com.hub.shared.domain.ResidentId
import java.time.Instant

interface EpisodePort {
    fun createEpisode(request: CreateEpisodePortRequest): EpisodePortResponse
    fun updateEpisode(episodeId: String, request: UpdateEpisodePortRequest)
    fun findById(episodeId: String): EpisodePortModel?
    fun save(episode: EpisodePortModel)
}

data class CreateEpisodePortRequest(
    val id: String,
    val residentId: String,
    val bedId: String,
    val severity: String,
    val title: String,
    val detail: String,
    val occurredAt: Instant,
)

data class UpdateEpisodePortRequest(
    val status: String? = null,
)

data class EpisodePortResponse(val id: String)

data class EpisodePortModel(
    val id: String,
    val severity: String,
    val escalationLevel: Int,
)
