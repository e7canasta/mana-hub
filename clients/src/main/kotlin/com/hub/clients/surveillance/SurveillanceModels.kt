package com.hub.clients.surveillance

import com.fasterxml.jackson.annotation.JsonProperty
import com.hub.clients.care.EpisodeNoteKind
import java.time.Instant

data class CreateEpisodeRequest(
    @JsonProperty("residentId") val residentId: String,
    @JsonProperty("bedId") val bedId: String? = null,
    val severity: EpisodeSeverity,
    val title: String? = null,
    val detail: String? = null,
    @JsonProperty("occurredAt") val occurredAt: Instant,
    @JsonProperty("evidenceKind") val evidenceKind: String? = null,
    @JsonProperty("evidenceRef") val evidenceRef: String? = null
)

data class EpisodeResponse(
    val id: String,
    @JsonProperty("residentId") val residentId: String,
    @JsonProperty("bedId") val bedId: String? = null,
    val severity: EpisodeSeverity,
    val status: String,
    val title: String? = null,
    val detail: String? = null,
    @JsonProperty("occurredAt") val occurredAt: Instant,
    @JsonProperty("escalationLevel") val escalationLevel: Int = 0,
    @JsonProperty("pending") val isPending: Boolean = true
)

data class AcknowledgeEpisodeRequest(
    @JsonProperty("actorId") val actorId: String
)

data class UpdateEpisodeRequest(
    val status: String? = null,
    val title: String? = null,
    val detail: String? = null
)

enum class EpisodeSeverity { INFO, WARNING, CRITICAL, EMERGENCY }

// ══════════════════════════════════════════════════════════════
//  ESTADO DEL EPISODIO — Vocabulary: §3.4
// ══════════════════════════════════════════════════════════════

enum class EpisodeStatus {
    PENDING,
    ACKNOWLEDGED,
    RESOLVED,
    AUTO_RESOLVED
}

// EpisodeNote models (for fluent DSL)
data class CreateEpisodeNoteRequest(
    @JsonProperty("episodeId") val episodeId: String,
    @JsonProperty("authorId") val authorId: String,
    val kind: EpisodeNoteKind,
    val body: String,
    @JsonProperty("timestamp") val timestamp: Instant = Instant.now()
)
