package com.hub.clients.history

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class IncidentDetectionResponse(
    val id: String,
    @JsonProperty("sourceRecordId") val sourceRecordId: String,
    @JsonProperty("residentId") val residentId: String,
    @JsonProperty("bedId") val bedId: String? = null,
    val kind: String,
    val severity: IncidentSeverity,
    @JsonProperty("occurredAt") val occurredAt: Instant,
    val narrative: String? = null,
    val source: String
)

data class IncidentReviewResponse(
    val id: String,
    @JsonProperty("incidentId") val incidentId: String,
    val status: String,
    @JsonProperty("detectionVerdict") val detectionVerdict: String? = null,
    @JsonProperty("reviewNote") val reviewNote: String? = null,
    @JsonProperty("resolvedAt") val resolvedAt: Instant? = null,
    @JsonProperty("actorId") val actorId: String
)

data class ReviewIncidentRequest(
    val status: String,
    @JsonProperty("detectionVerdict") val detectionVerdict: String? = null,
    @JsonProperty("reviewNote") val reviewNote: String? = null,
    @JsonProperty("actorId") val actorId: String
)

enum class IncidentSeverity { LOW, MEDIUM, HIGH, CRITICAL }
