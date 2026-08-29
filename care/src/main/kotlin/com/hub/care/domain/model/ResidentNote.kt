package com.hub.care.domain.model

import com.hub.shared.domain.Identifier
import com.hub.shared.domain.ResidentId
import java.time.Instant

enum class ResidentNoteKind {
    CARE,
    CLINICAL,
    INSIGHT,
    PATTERN,
    OBSERVATION,
    SUMMARY;

    companion object {
        fun from(value: String): ResidentNoteKind = when (value.uppercase()) {
            "CARE" -> CARE
            "CLINICAL" -> CLINICAL
            "INSIGHT" -> INSIGHT
            "PATTERN" -> PATTERN
            "OBSERVATION" -> OBSERVATION
            "SUMMARY" -> SUMMARY
            else -> throw IllegalArgumentException("Unknown resident note kind: $value")
        }
    }
}

data class ResidentNote(
    val id: Identifier,
    val residentId: ResidentId,
    val authorId: String,
    val kind: ResidentNoteKind,
    val body: String,
    val sourceEventId: String?,
    val timestamp: Instant,
    val createdAt: Instant
) {
    companion object {
        fun create(
            residentId: ResidentId,
            authorId: String,
            kind: ResidentNoteKind,
            body: String,
            sourceEventId: String? = null,
            timestamp: Instant = Instant.now()
        ): ResidentNote = ResidentNote(
            id = Identifier.random(),
            residentId = residentId,
            authorId = authorId,
            kind = kind,
            body = body,
            sourceEventId = sourceEventId,
            timestamp = timestamp,
            createdAt = Instant.now()
        )
    }
}
