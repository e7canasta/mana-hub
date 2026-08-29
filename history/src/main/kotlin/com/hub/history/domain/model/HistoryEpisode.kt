package com.hub.history.domain.model

import com.hub.shared.domain.AggregateRoot
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.BedId
import java.time.Instant

class HistoryEpisode private constructor(
    override val id: HistoryEpisodeId,
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
    override var version: Long
) : AggregateRoot<HistoryEpisodeId>() {

    companion object {
        fun create(
            sourceRecordId: String, residentId: ResidentId, bedId: BedId?, kind: EpisodeKind,
            severity: HistoryEpisodeSeverity, occurredAt: Instant, source: EventSource
        ): HistoryEpisode {
            require(sourceRecordId.isNotBlank()) { "sourceRecordId must not be blank" }
            require(occurredAt.isBefore(Instant.now().plusSeconds(3600))) { "occurredAt cannot be in the far future" }
            return HistoryEpisode(
                id = HistoryEpisodeId.random(), sourceRecordId = sourceRecordId, residentId = residentId,
                bedId = bedId, sourceAlertId = null, kind = kind, severity = severity,
                occurredAt = occurredAt, activity = null, injuryStatus = null,
                selfRecovery = false, responseSeconds = null, narrative = null,
                source = source, modelVersion = null, confidence = null,
                provenanceJson = "{}", version = 0
            )
        }

        fun reconstitute(data: HistoryEpisodeData): HistoryEpisode = HistoryEpisode(
            id = data.id, sourceRecordId = data.sourceRecordId, residentId = data.residentId,
            bedId = data.bedId, sourceAlertId = data.sourceAlertId, kind = data.kind,
            severity = data.severity, occurredAt = data.occurredAt, activity = data.activity,
            injuryStatus = data.injuryStatus, selfRecovery = data.selfRecovery,
            responseSeconds = data.responseSeconds, narrative = data.narrative,
            source = data.source, modelVersion = data.modelVersion, confidence = data.confidence,
            provenanceJson = data.provenanceJson, version = data.version
        )
    }
}
