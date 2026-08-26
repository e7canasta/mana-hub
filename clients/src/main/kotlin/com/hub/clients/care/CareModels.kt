package com.hub.clients.care

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import java.time.Instant

// ══════════════════════════════════════════════════════════════
//  ROUND
// ══════════════════════════════════════════════════════════════

data class CreateRoundRequest(
    @JsonProperty("wingId") val wingId: String,
    @JsonProperty("scheduledFor") val scheduledFor: Instant? = null
)

data class RoundResponse(
    val id: String,
    @JsonProperty("wingId") val wingId: String,
    val status: RoundStatus,
    @JsonProperty("scheduledFor") val scheduledFor: Instant? = null,
    @JsonProperty("startedAt") val startedAt: Instant? = null,
    @JsonProperty("completedAt") val completedAt: Instant? = null,
    @JsonProperty("startedBy") val startedBy: String? = null,
    @JsonProperty("completedBy") val completedBy: String? = null
)

data class RoundTaskResponse(
    val id: String,
    @JsonProperty("roundId") val roundId: String,
    @JsonProperty("residentId") val residentId: String,
    @JsonProperty("bedId") val bedId: String? = null,
    val status: String,
    val note: String? = null,
    @JsonProperty("completedAt") val completedAt: Instant? = null,
    @JsonProperty("completedBy") val completedBy: String? = null
)

data class UpdateRoundTaskRequest(
    val note: String? = null,
    @JsonProperty("completedBy") val completedBy: String? = null
)

enum class RoundStatus { IN_PROGRESS, COMPLETED, CANCELLED }

// ══════════════════════════════════════════════════════════════
//  RESIDENT NOTE TYPES — Vocabulary: §3.3
// ══════════════════════════════════════════════════════════════

enum class ResidentNoteType(@JsonValue val apiValue: String) {
    CARE("CARE"),
    CLINICAL("CLINICAL"),
    INSIGHT("INSIGHT"),
    PATTERN("PATTERN"),
    OBSERVATION("OBSERVATION"),
    SUMMARY("SUMMARY")
}

// ══════════════════════════════════════════════════════════════
//  EPISODE NOTE KIND
// ══════════════════════════════════════════════════════════════

enum class EpisodeNoteKind(@JsonValue val apiValue: String) {
    ACKNOWLEDGEMENT("ACKNOWLEDGEMENT"),
    RESOLUTION("RESOLUTION"),
    CLINICAL_NOTE("CLINICAL_NOTE")
}

// ══════════════════════════════════════════════════════════════
//  SHIFT NOTE KIND
// ══════════════════════════════════════════════════════════════

enum class ShiftNoteKind(@JsonValue val apiValue: String) {
    SHIFT_SUMMARY("SHIFT_SUMMARY"),
    INCIDENT_REPORT("INCIDENT_REPORT"),
    GENERAL("GENERAL")
}

// ══════════════════════════════════════════════════════════════
//  RESIDENT NOTE (all kinds: CARE, CLINICAL, INSIGHT, etc.)
// ══════════════════════════════════════════════════════════════

data class CreateResidentNoteRequest(
    @JsonProperty("residentId") val residentId: String,
    @JsonProperty("authorId") val authorId: String,
    val kind: ResidentNoteType,
    val body: String,
    @JsonProperty("sourceEventId") val sourceEventId: String? = null,
    @JsonProperty("timestamp") val timestamp: Instant = Instant.now()
)

data class ResidentNoteResponse(
    val id: String,
    @JsonProperty("residentId") val residentId: String,
    @JsonProperty("authorId") val authorId: String,
    val kind: ResidentNoteType,
    val body: String,
    @JsonProperty("sourceEventId") val sourceEventId: String?,
    @JsonProperty("timestamp") val timestamp: Instant,
    @JsonProperty("createdAt") val createdAt: Instant
)

// ══════════════════════════════════════════════════════════════
//  EPISODE NOTE — lives in SurveillanceModels.kt
// ══════════════════════════════════════════════════════════════

data class EpisodeNoteResponse(
    val id: String,
    @JsonProperty("episodeId") val episodeId: String,
    @JsonProperty("authorId") val authorId: String,
    val kind: EpisodeNoteKind,
    val body: String,
    @JsonProperty("timestamp") val timestamp: Instant,
    @JsonProperty("createdAt") val createdAt: Instant
)

// ══════════════════════════════════════════════════════════════
//  SHIFT NOTE
// ══════════════════════════════════════════════════════════════

data class CreateShiftNoteRequest(
    @JsonProperty("facilityId") val facilityId: String,
    @JsonProperty("wingId") val wingId: String? = null,
    @JsonProperty("shiftKey") val shiftKey: String,
    @JsonProperty("shiftDate") val shiftDate: String,
    @JsonProperty("authorId") val authorId: String,
    val kind: ShiftNoteKind,
    val body: String,
    @JsonProperty("timestamp") val timestamp: Instant = Instant.now()
)

data class ShiftNoteResponse(
    val id: String,
    @JsonProperty("facilityId") val facilityId: String,
    @JsonProperty("wingId") val wingId: String?,
    @JsonProperty("shiftKey") val shiftKey: String,
    @JsonProperty("shiftDate") val shiftDate: String,
    @JsonProperty("authorId") val authorId: String,
    val kind: ShiftNoteKind,
    val body: String,
    @JsonProperty("timestamp") val timestamp: Instant,
    @JsonProperty("createdAt") val createdAt: Instant
)
