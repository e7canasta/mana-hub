package com.hub.clients.panel

import com.hub.clients.core.HttpApi
import com.hub.shared.panel.*

class ResidentScope internal constructor(private val http: HttpApi) {

    fun list(): List<ResidentRailDto> =
        http.get("/api/v1/panel/residents", Array<ResidentRailDto>::class.java).toList()

    fun detail(id: String): ResidentRailDto =
        http.get("/api/v1/panel/residents/$id", ResidentRailDto::class.java)

    fun notes(residentId: String): ResidentNotesResponse =
        http.get("/api/v1/panel/residents/$residentId/notes", ResidentNotesResponse::class.java)

    fun createNote(residentId: String, kind: NoteKind, body: String, authorId: String): NoteCreatedResponse =
        http.post(
            "/api/v1/panel/residents/$residentId/notes",
            CreateResidentNoteRequest(kind, body, authorId),
            NoteCreatedResponse::class.java,
        )
}
