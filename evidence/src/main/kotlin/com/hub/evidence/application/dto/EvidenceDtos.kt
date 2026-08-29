package com.hub.evidence.application.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class CreateEvidenceRequest(
    val bedId: String,
    val residentId: String,
    val evidenceType: String,
    val category: String? = null,
    val timestamp: Instant
)

data class EvidenceResponse(
    val id: String,
    val bedId: String,
    val residentId: String,
    val evidenceType: String,
    val category: String?,
    val riskLevel: String?,
    val timestamp: Instant
)

data class TimelineResponse(
    val id: String,
    val bedId: String,
    val residentId: String,
    val windowStart: Instant,
    val windowEnd: Instant?,
    @JsonProperty("isOpen") val isOpen: Boolean
)

data class ClipWindowResponse(
    val id: String,
    val bedId: String,
    val residentId: String,
    val startedAt: Instant,
    val endedAt: Instant?,
    val state: String
)
