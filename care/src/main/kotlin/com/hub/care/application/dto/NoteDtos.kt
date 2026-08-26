package com.hub.care.application.dto

import com.hub.care.domain.model.EpisodeNoteKind
import com.hub.care.domain.model.ResidentNoteKind
import com.hub.care.domain.model.ShiftNoteKind
import java.time.Instant

data class CreateResidentNoteRequest(
    val residentId: String,
    val authorId: String,
    val kind: ResidentNoteKind,
    val body: String,
    val sourceEventId: String? = null,
    val timestamp: Instant = Instant.now()
)

data class ResidentNoteResponse(
    val id: String,
    val residentId: String,
    val authorId: String,
    val kind: ResidentNoteKind,
    val body: String,
    val sourceEventId: String?,
    val timestamp: Instant,
    val createdAt: Instant
)

data class CreateEpisodeNoteRequest(
    val episodeId: String,
    val authorId: String,
    val kind: EpisodeNoteKind,
    val body: String,
    val timestamp: Instant = Instant.now()
)

data class EpisodeNoteResponse(
    val id: String,
    val episodeId: String,
    val authorId: String,
    val kind: EpisodeNoteKind,
    val body: String,
    val timestamp: Instant,
    val createdAt: Instant
)

data class CreateShiftNoteRequest(
    val facilityId: String,
    val wingId: String? = null,
    val shiftKey: String,
    val shiftDate: String,
    val authorId: String,
    val kind: ShiftNoteKind,
    val body: String,
    val timestamp: Instant = Instant.now()
)

data class ShiftNoteResponse(
    val id: String,
    val facilityId: String,
    val wingId: String?,
    val shiftKey: String,
    val shiftDate: String,
    val authorId: String,
    val kind: ShiftNoteKind,
    val body: String,
    val timestamp: Instant,
    val createdAt: Instant
)
