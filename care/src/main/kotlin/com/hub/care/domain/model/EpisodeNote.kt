package com.hub.care.domain.model

import com.hub.shared.domain.Identifier
import java.time.Instant
import java.util.UUID

@JvmInline
value class EpisodeId(val value: String) {
    companion object {
        fun from(value: String): EpisodeId = EpisodeId(value)
        fun random(): EpisodeId = EpisodeId(UUID.randomUUID().toString())
    }
}

enum class EpisodeNoteKind {
    ACKNOWLEDGEMENT,
    RESOLUTION,
    CLINICAL_NOTE;

    companion object {
        fun from(value: String): EpisodeNoteKind = when (value.uppercase()) {
            "ACKNOWLEDGEMENT" -> ACKNOWLEDGEMENT
            "RESOLUTION" -> RESOLUTION
            "CLINICAL_NOTE" -> CLINICAL_NOTE
            else -> throw IllegalArgumentException("Unknown episode note kind: $value")
        }
    }
}

data class EpisodeNote(
    val id: Identifier,
    val episodeId: EpisodeId,
    val authorId: String,
    val kind: EpisodeNoteKind,
    val body: String,
    val timestamp: Instant,
    val createdAt: Instant
) {
    companion object {
        fun create(
            episodeId: EpisodeId,
            authorId: String,
            kind: EpisodeNoteKind,
            body: String,
            timestamp: Instant = Instant.now()
        ): EpisodeNote = EpisodeNote(
            id = Identifier.random(),
            episodeId = episodeId,
            authorId = authorId,
            kind = kind,
            body = body,
            timestamp = timestamp,
            createdAt = Instant.now()
        )
    }
}
