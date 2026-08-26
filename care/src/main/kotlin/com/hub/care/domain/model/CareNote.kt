package com.hub.care.domain.model

import com.hub.population.domain.model.ResidentId
import java.time.Instant
import java.util.UUID

@JvmInline
value class CareNoteId(val value: String) {
    companion object {
        fun from(value: String): CareNoteId = CareNoteId(value)
        fun random(): CareNoteId = CareNoteId(UUID.randomUUID().toString())
    }
}

data class CareNote(
    val id: CareNoteId,
    val residentId: ResidentId,
    val authorId: String,
    val kind: String,
    val body: String,
    val durationMin: Int?,
    val createdAt: Instant = Instant.now()
) {
    companion object {
        fun create(residentId: ResidentId, authorId: String, kind: String, body: String, durationMin: Int?): CareNote = CareNote(
            id = CareNoteId.random(), residentId = residentId, authorId = authorId,
            kind = kind, body = body, durationMin = durationMin
        )
    }
}
