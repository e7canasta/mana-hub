package com.hub.care.application.dto

import com.hub.care.domain.model.CareNoteKind
import com.hub.care.domain.model.RoundStatus
import com.hub.care.domain.model.RoundTaskStatus
import java.time.Instant

data class CreateRoundRequest(
    val wingId: String,
    val scheduledFor: Instant? = null
)

data class RoundResponse(
    val id: String,
    val wingId: String,
    val status: RoundStatus,
    val scheduledFor: Instant?,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val startedBy: String?,
    val completedBy: String?
)

data class RoundTaskResponse(
    val id: String,
    val roundId: String,
    val residentId: String,
    val bedId: String?,
    val status: RoundTaskStatus,
    val note: String?,
    val completedAt: Instant?,
    val completedBy: String?
)

data class UpdateRoundTaskRequest(
    val note: String? = null,
    val completedBy: String? = null
)

data class CreateCareNoteRequest(
    val residentId: String,
    val authorId: String,
    val kind: CareNoteKind = CareNoteKind.GENERAL,
    val body: String,
    val durationMin: Int? = null
)

data class CareNoteResponse(
    val id: String,
    val residentId: String,
    val authorId: String,
    val kind: CareNoteKind,
    val body: String,
    val durationMin: Int?,
    val createdAt: Instant
)
