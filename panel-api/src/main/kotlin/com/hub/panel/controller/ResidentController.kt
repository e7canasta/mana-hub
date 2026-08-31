package com.hub.panel.controller

import com.hub.panel.command.PanelCommandService
import com.hub.panel.projection.PanelProjectionService
import com.hub.shared.panel.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController("panelResidentController")
@RequestMapping("/api/v1/panel/residents")
class PanelResidentController(
    private val projection: PanelProjectionService,
    private val command: PanelCommandService,
) {

    @GetMapping
    fun list(): List<ResidentRailDto> = projection.residentRail()

    @GetMapping("/{id}")
    fun detail(@PathVariable id: String): ResponseEntity<ResidentRailDto> =
        projection.residentDetail(id)?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @GetMapping("/{id}/notes")
    fun notes(@PathVariable id: String): ResponseEntity<ResidentNotesResponse> {
        return ResponseEntity.ok(ResidentNotesResponse(notes = emptyList()))
    }

    @PostMapping("/{id}/notes")
    fun createNote(
        @PathVariable id: String,
        @RequestBody body: CreateResidentNoteRequest,
    ): ResponseEntity<NoteCreatedResponse> {
        val result = command.createResidentNote(
            residentId = id,
            kind = body.kind,
            body = body.body,
            authorId = body.authorId,
        )
        return ResponseEntity.ok(result)
    }
}
