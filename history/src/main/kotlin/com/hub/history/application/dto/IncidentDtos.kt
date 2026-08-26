package com.hub.history.application.dto

import com.hub.history.domain.model.IncidentSeverity
import java.time.Instant

data class IngestIncidentRequest(
    val sourceRecordId: String,
    val residentId: String,
    val bedId: String? = null,
    val kind: String,
    val severity: IncidentSeverity,
    val occurredAt: Instant,
    val location: String? = null,
    val activity: String? = null,
    val narrative: String? = null,
    val source: String = "internal"
)

data class IncidentDetectionResponse(
    val id: String,
    val sourceRecordId: String,
    val residentId: String,
    val bedId: String?,
    val kind: String,
    val severity: IncidentSeverity,
    val occurredAt: Instant,
    val narrative: String?,
    val source: String
)

data class IncidentReviewResponse(
    val id: String,
    val incidentId: String,
    val status: String,
    val detectionVerdict: String?,
    val reviewNote: String?,
    val resolvedAt: Instant?,
    val actorId: String
)

data class ReviewIncidentRequest(
    val status: String,
    val detectionVerdict: String? = null,
    val reviewNote: String? = null,
    val actorId: String
)
