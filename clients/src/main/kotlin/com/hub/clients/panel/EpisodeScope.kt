package com.hub.clients.panel

import com.hub.clients.core.HttpApi
import com.hub.shared.panel.*

class EpisodeScope internal constructor(private val http: HttpApi) {

    fun feed(): EpisodeFeedDto =
        http.get("/api/v1/panel/episodes", EpisodeFeedDto::class.java)

    fun detail(id: String): EpisodeDetailDto =
        http.get("/api/v1/panel/episodes/$id", EpisodeDetailDto::class.java)

    fun review(episodeId: String, verdict: EpisodeVerdict, note: String?, actorId: String): ReviewEpisodeResponse =
        http.post(
            "/api/v1/panel/episodes/$episodeId/review",
            ReviewEpisodeRequest(verdict, note, actorId),
            ReviewEpisodeResponse::class.java,
        )

    fun notes(episodeId: String): EpisodeNotesResponse =
        http.get("/api/v1/panel/episodes/$episodeId/notes", EpisodeNotesResponse::class.java)

    fun createNote(episodeId: String, kind: NoteKind, body: String, authorId: String): NoteCreatedResponse =
        http.post(
            "/api/v1/panel/episodes/$episodeId/notes",
            CreateEpisodeNoteRequest(kind, body, authorId),
            NoteCreatedResponse::class.java,
        )

    fun interventions(episodeId: String): EpisodeInterventionsResponse =
        http.get("/api/v1/panel/episodes/$episodeId/interventions", EpisodeInterventionsResponse::class.java)
}
