package com.hub.history.domain.model

import com.hub.shared.domain.AggregateRoot
import com.hub.population.domain.model.ResidentId
import com.hub.residence.domain.model.BedId
import java.time.Instant

class HistoryEpisode private constructor(
    override val id: HistoryEpisodeId,
    val sourceRecordId: String,
    val residentId: ResidentId,
    val bedId: BedId?,
    val sourceAlertId: String?,
    val kind: String,
    val severity: HistoryEpisodeSeverity,
    val occurredAt: Instant,
    val location: String?,
    val activity: String?,
    val injuryStatus: String?,
    val selfRecovery: Boolean,
    val responseSeconds: Int?,
    val narrative: String?,
    val interventionsJson: String,
    val source: String,
    val modelVersion: String?,
    val confidence: Double?,
    val provenanceJson: String,
    override var version: Long
) : AggregateRoot<HistoryEpisodeId>() {

    companion object {
        fun create(
            sourceRecordId: String, residentId: ResidentId, bedId: BedId?, kind: String,
            severity: HistoryEpisodeSeverity, occurredAt: Instant, source: String
        ): HistoryEpisode = HistoryEpisode(
            id = HistoryEpisodeId.random(), sourceRecordId = sourceRecordId, residentId = residentId,
            bedId = bedId, sourceAlertId = null, kind = kind, severity = severity,
            occurredAt = occurredAt, location = null, activity = null, injuryStatus = null,
            selfRecovery = false, responseSeconds = null, narrative = null,
            interventionsJson = "[]", source = source, modelVersion = null, confidence = null,
            provenanceJson = "{}", version = 0
        )

        fun reconstitute(
            id: HistoryEpisodeId, sourceRecordId: String, residentId: ResidentId, bedId: BedId?,
            sourceAlertId: String?, kind: String, severity: HistoryEpisodeSeverity, occurredAt: Instant,
            location: String?, activity: String?, injuryStatus: String?, selfRecovery: Boolean,
            responseSeconds: Int?, narrative: String?, interventionsJson: String, source: String,
            modelVersion: String?, confidence: Double?, provenanceJson: String, version: Long
        ): HistoryEpisode = HistoryEpisode(
            id, sourceRecordId, residentId, bedId, sourceAlertId, kind, severity, occurredAt,
            location, activity, injuryStatus, selfRecovery, responseSeconds, narrative,
            interventionsJson, source, modelVersion, confidence, provenanceJson, version
        )
    }
}
