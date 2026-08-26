package com.hub.history.domain.model

import com.hub.shared.domain.AggregateRoot
import java.time.Instant

class IncidentReview private constructor(
    override val id: IncidentId,
    val incidentId: IncidentId,
    val status: String,
    val detectionVerdict: String?,
    val reviewNote: String?,
    val resolvedAt: Instant?,
    val actorId: String,
    override var version: Long
) : AggregateRoot<IncidentId>() {

    companion object {
        fun create(incidentId: IncidentId, actorId: String): IncidentReview = IncidentReview(
            id = IncidentId.random(), incidentId = incidentId, status = "pending",
            detectionVerdict = null, reviewNote = null, resolvedAt = null,
            actorId = actorId, version = 0
        )

        fun reconstitute(
            id: IncidentId, incidentId: IncidentId, status: String, detectionVerdict: String?,
            reviewNote: String?, resolvedAt: Instant?, actorId: String, version: Long
        ): IncidentReview = IncidentReview(id, incidentId, status, detectionVerdict, reviewNote, resolvedAt, actorId, version)
    }
}
