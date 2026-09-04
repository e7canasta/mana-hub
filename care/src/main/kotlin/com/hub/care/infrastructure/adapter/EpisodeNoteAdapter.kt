package com.hub.care.infrastructure.adapter

import com.hub.care.application.dto.CreateEpisodeNoteRequest
import com.hub.care.application.service.NoteApplicationService
import com.hub.care.domain.model.EpisodeNoteKind
import com.hub.shared.domain.port.CreateEpisodeNotePortRequest
import com.hub.shared.domain.port.EpisodeNotePort
import com.hub.shared.domain.port.EpisodeNotePortResponse
import org.springframework.stereotype.Component

@Component
class EpisodeNoteAdapter(
    private val noteService: NoteApplicationService,
) : EpisodeNotePort {

    override fun createNote(request: CreateEpisodeNotePortRequest): EpisodeNotePortResponse {
        val kind = try {
            EpisodeNoteKind.valueOf(request.kind)
        } catch (_: Exception) {
            EpisodeNoteKind.CLINICAL_NOTE
        }
        val response = noteService.createEpisodeNote(
            CreateEpisodeNoteRequest(
                episodeId = request.episodeId,
                authorId = request.authorId,
                kind = kind,
                body = request.body,
                timestamp = request.timestamp,
            )
        )
        return EpisodeNotePortResponse(id = response.id, episodeId = response.episodeId)
    }
}
