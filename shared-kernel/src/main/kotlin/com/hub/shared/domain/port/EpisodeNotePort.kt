package com.hub.shared.domain.port

import java.time.Instant

interface EpisodeNotePort {
    fun createNote(request: CreateEpisodeNotePortRequest): EpisodeNotePortResponse
}

data class CreateEpisodeNotePortRequest(
    val episodeId: String,
    val authorId: String,
    val kind: String,
    val body: String,
    val timestamp: Instant = Instant.now(),
)

data class EpisodeNotePortResponse(
    val id: String,
    val episodeId: String,
)
