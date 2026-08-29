package com.hub.history.domain.model

import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.BedId
import java.time.Instant

data class HistoryEpisodeData(
    val id: HistoryEpisodeId,
    val sourceRecordId: String,
    val residentId: ResidentId,
    val bedId: BedId?,
    val sourceAlertId: String?,
    val kind: EpisodeKind,
    val severity: HistoryEpisodeSeverity,
    val occurredAt: Instant,
    val activity: String?,
    val injuryStatus: String?,
    val selfRecovery: Boolean,
    val responseSeconds: Int?,
    val narrative: String?,
    val source: EventSource,
    val modelVersion: String?,
    val confidence: Double?,
    val provenanceJson: String,
    val version: Long
)
