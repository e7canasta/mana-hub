package com.hub.care.api.rest

import com.hub.care.application.dto.*
import com.hub.care.application.service.NoteApplicationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class NoteController(
    private val noteApplicationService: NoteApplicationService
) {

    // ResidentNote endpoints
    @PostMapping("/residents/{residentId}/notes")
    fun createResidentNote(
        @PathVariable residentId: String,
        @Valid @RequestBody request: CreateResidentNoteRequest
    ): ResponseEntity<ResidentNoteResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            noteApplicationService.createResidentNote(request.copy(residentId = residentId))
        )
    }

    @GetMapping("/residents/{residentId}/notes")
    fun getResidentNotes(@PathVariable residentId: String): ResponseEntity<List<ResidentNoteResponse>> {
        return ResponseEntity.ok(noteApplicationService.getResidentNotes(residentId))
    }

    // EpisodeNote endpoints
    @PostMapping("/episodes/{episodeId}/notes")
    fun createEpisodeNote(
        @PathVariable episodeId: String,
        @Valid @RequestBody request: CreateEpisodeNoteBody
    ): ResponseEntity<EpisodeNoteResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            noteApplicationService.createEpisodeNote(
                CreateEpisodeNoteRequest(
                    episodeId = episodeId,
                    authorId = request.authorId,
                    kind = request.kind,
                    body = request.body,
                    timestamp = request.timestamp ?: java.time.Instant.now(),
                ),
            )
        )
    }

    @GetMapping("/episodes/{episodeId}/notes")
    fun getEpisodeNotes(@PathVariable episodeId: String): ResponseEntity<List<EpisodeNoteResponse>> {
        return ResponseEntity.ok(noteApplicationService.getEpisodeNotes(episodeId))
    }

    // ShiftNote endpoints
    @PostMapping("/shift-notes")
    fun createShiftNote(
        @Valid @RequestBody request: CreateShiftNoteRequest
    ): ResponseEntity<ShiftNoteResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            noteApplicationService.createShiftNote(request)
        )
    }

    @GetMapping("/facilities/{facilityId}/shift-notes")
    fun getShiftNotes(
        @PathVariable facilityId: String,
        @RequestParam shiftDate: String
    ): ResponseEntity<List<ShiftNoteResponse>> {
        return ResponseEntity.ok(noteApplicationService.getShiftNotes(facilityId, shiftDate))
    }

    @GetMapping("/wings/{wingId}/shift-notes")
    fun getWingShiftNotes(
        @PathVariable wingId: String,
        @RequestParam shiftDate: String
    ): ResponseEntity<List<ShiftNoteResponse>> {
        return ResponseEntity.ok(noteApplicationService.getWingShiftNotes(wingId, shiftDate))
    }
}
