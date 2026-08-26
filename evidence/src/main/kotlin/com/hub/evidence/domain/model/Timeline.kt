package com.hub.evidence.domain.model

import com.hub.shared.domain.AggregateRoot
import com.hub.population.domain.model.ResidentId
import com.hub.residence.domain.model.BedId
import java.time.Instant

class Timeline private constructor(
    override val id: EvidenceId,
    val bedId: BedId,
    val residentId: ResidentId,
    val anchorEventId: String?,
    val anchorEventJson: String?,
    val beforeEventsJson: String,
    val afterEventsJson: String,
    val windowStart: Instant,
    val windowEnd: Instant?,
    val closedAt: Instant?,
    override var version: Long
) : AggregateRoot<EvidenceId>() {

    val isOpen: Boolean get() = closedAt == null

    fun close(): Timeline {
        require(isOpen) { "Timeline is already closed" }
        return reconstitute(
            id = id, bedId = bedId, residentId = residentId, anchorEventId = anchorEventId,
            anchorEventJson = anchorEventJson, beforeEventsJson = beforeEventsJson,
            afterEventsJson = afterEventsJson, windowStart = windowStart, windowEnd = windowEnd,
            closedAt = Instant.now(), version = version + 1
        )
    }

    companion object {
        fun create(bedId: BedId, residentId: ResidentId, windowStart: Instant): Timeline = Timeline(
            id = EvidenceId.random(), bedId = bedId, residentId = residentId, anchorEventId = null,
            anchorEventJson = null, beforeEventsJson = "[]", afterEventsJson = "[]",
            windowStart = windowStart, windowEnd = null, closedAt = null, version = 0
        )

        fun reconstitute(
            id: EvidenceId, bedId: BedId, residentId: ResidentId, anchorEventId: String?,
            anchorEventJson: String?, beforeEventsJson: String, afterEventsJson: String,
            windowStart: Instant, windowEnd: Instant?, closedAt: Instant?, version: Long
        ): Timeline = Timeline(id, bedId, residentId, anchorEventId, anchorEventJson, beforeEventsJson, afterEventsJson, windowStart, windowEnd, closedAt, version)
    }
}
