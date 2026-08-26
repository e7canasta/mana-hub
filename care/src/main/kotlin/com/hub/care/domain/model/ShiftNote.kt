package com.hub.care.domain.model

import com.hub.shared.domain.Identifier
import java.time.Instant

enum class ShiftNoteKind {
    SHIFT_SUMMARY,
    INCIDENT_REPORT,
    GENERAL;

    companion object {
        fun from(value: String): ShiftNoteKind = when (value.uppercase()) {
            "SHIFT_SUMMARY" -> SHIFT_SUMMARY
            "INCIDENT_REPORT" -> INCIDENT_REPORT
            "GENERAL" -> GENERAL
            else -> throw IllegalArgumentException("Unknown shift note kind: $value")
        }
    }
}

data class ShiftNote(
    val id: Identifier,
    val facilityId: String,
    val wingId: String?,
    val shiftKey: String,
    val shiftDate: String,
    val authorId: String,
    val kind: ShiftNoteKind,
    val body: String,
    val timestamp: Instant,
    val createdAt: Instant
) {
    companion object {
        fun create(
            facilityId: String,
            wingId: String?,
            shiftKey: String,
            shiftDate: String,
            authorId: String,
            kind: ShiftNoteKind,
            body: String,
            timestamp: Instant = Instant.now()
        ): ShiftNote = ShiftNote(
            id = Identifier.random(),
            facilityId = facilityId,
            wingId = wingId,
            shiftKey = shiftKey,
            shiftDate = shiftDate,
            authorId = authorId,
            kind = kind,
            body = body,
            timestamp = timestamp,
            createdAt = Instant.now()
        )
    }
}
