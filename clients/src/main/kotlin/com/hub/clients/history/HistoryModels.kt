package com.hub.clients.history

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class HistoryEpisodeResponse(
    val id: String,
    @JsonProperty("sourceRecordId") val sourceRecordId: String,
    @JsonProperty("residentId") val residentId: String,
    @JsonProperty("bedId") val bedId: String? = null,
    val kind: String,
    val severity: HistoryEpisodeSeverity,
    @JsonProperty("occurredAt") val occurredAt: Instant,
    val narrative: String? = null,
    val source: String
)

data class HistoryEpisodeReviewResponse(
    val id: String,
    @JsonProperty("episodeId") val episodeId: String,
    val status: String,
    @JsonProperty("detectionVerdict") val detectionVerdict: String? = null,
    @JsonProperty("reviewNote") val reviewNote: String? = null,
    @JsonProperty("resolvedAt") val resolvedAt: Instant? = null,
    @JsonProperty("actorId") val actorId: String
)

data class ReviewHistoryEpisodeRequest(
    val status: String,
    @JsonProperty("detectionVerdict") val detectionVerdict: String? = null,
    @JsonProperty("reviewNote") val reviewNote: String? = null,
    @JsonProperty("actorId") val actorId: String
)

enum class HistoryEpisodeSeverity { LOW, MEDIUM, HIGH, CRITICAL }
