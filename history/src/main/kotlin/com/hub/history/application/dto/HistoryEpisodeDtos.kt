package com.hub.history.application.dto

import com.hub.history.domain.model.EpisodeKind
import com.hub.history.domain.model.EventSource
import com.hub.history.domain.model.HistoryEpisodeSeverity
import java.time.Instant

data class IngestHistoryEpisodeRequest(
    val sourceRecordId: String,
    val residentId: String,
    val bedId: String? = null,
    val kind: EpisodeKind,
    val severity: HistoryEpisodeSeverity,
    val occurredAt: Instant,
    val activity: String? = null,
    val narrative: String? = null,
    val source: EventSource = EventSource.OTHER
)

data class HistoryEpisodeResponse(
    val id: String,
    val sourceRecordId: String,
    val residentId: String,
    val bedId: String?,
    val kind: EpisodeKind,
    val severity: HistoryEpisodeSeverity,
    val occurredAt: Instant,
    val narrative: String?,
    val source: EventSource
)

data class HistoryEpisodeReviewResponse(
    val id: String,
    val episodeId: String,
    val status: String,
    val detectionVerdict: String?,
    val reviewNote: String?,
    val resolvedAt: Instant?,
    val actorId: String
)

data class ReviewHistoryEpisodeRequest(
    val status: String,
    val detectionVerdict: String? = null,
    val reviewNote: String? = null,
    val actorId: String
)
