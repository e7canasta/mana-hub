package com.hub.clients.evidence

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class CreateEvidenceRequest(
    @JsonProperty("bedId") val bedId: String,
    @JsonProperty("residentId") val residentId: String,
    @JsonProperty("evidenceType") val evidenceType: String,
    val category: String? = null,
    val timestamp: Instant,
    @JsonProperty("episodeId") val episodeId: String? = null
)

data class EvidenceResponse(
    val id: String,
    @JsonProperty("bedId") val bedId: String,
    @JsonProperty("residentId") val residentId: String,
    @JsonProperty("evidenceType") val evidenceType: String,
    val category: String? = null,
    @JsonProperty("riskLevel") val riskLevel: String? = null,
    val timestamp: Instant
)

data class TimelineResponse(
    val id: String,
    @JsonProperty("bedId") val bedId: String,
    @JsonProperty("residentId") val residentId: String,
    @JsonProperty("windowStart") val windowStart: Instant,
    @JsonProperty("windowEnd") val windowEnd: Instant? = null,
    @JsonProperty("open") val isOpen: Boolean
)

data class ClipWindowResponse(
    val id: String,
    @JsonProperty("bedId") val bedId: String,
    @JsonProperty("residentId") val residentId: String,
    @JsonProperty("startedAt") val startedAt: Instant,
    @JsonProperty("endedAt") val endedAt: Instant? = null,
    val state: String
)
