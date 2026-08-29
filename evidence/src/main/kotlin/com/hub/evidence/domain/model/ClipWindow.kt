package com.hub.evidence.domain.model

import com.hub.shared.domain.AggregateRoot
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.BedId
import java.time.Instant

class ClipWindow private constructor(
    override val id: EvidenceId,
    val bedId: BedId,
    val residentId: ResidentId,
    val startedAt: Instant,
    val endedAt: Instant?,
    val timeoutMinutes: Int,
    val eventsJson: String,
    val state: String,
    val closeConditionJson: String?,
    val closedAt: Instant?,
    override var version: Long
) : AggregateRoot<EvidenceId>() {

    val isOpen: Boolean get() = state == "open"

    fun close(): ClipWindow {
        require(isOpen) { "ClipWindow is already closed" }
        val now = Instant.now()
        return reconstitute(
            id = id, bedId = bedId, residentId = residentId, startedAt = startedAt,
            endedAt = now, timeoutMinutes = timeoutMinutes, eventsJson = eventsJson,
            state = "closed", closeConditionJson = closeConditionJson, closedAt = now,
            version = version + 1
        )
    }

    companion object {
        fun create(bedId: BedId, residentId: ResidentId, timeoutMinutes: Int = 5): ClipWindow = ClipWindow(
            id = EvidenceId.random(), bedId = bedId, residentId = residentId, startedAt = Instant.now(),
            endedAt = null, timeoutMinutes = timeoutMinutes, eventsJson = "[]", state = "open",
            closeConditionJson = null, closedAt = null, version = 0
        )

        fun reconstitute(
            id: EvidenceId, bedId: BedId, residentId: ResidentId, startedAt: Instant,
            endedAt: Instant?, timeoutMinutes: Int, eventsJson: String, state: String,
            closeConditionJson: String?, closedAt: Instant?, version: Long
        ): ClipWindow = ClipWindow(id, bedId, residentId, startedAt, endedAt, timeoutMinutes, eventsJson, state, closeConditionJson, closedAt, version)
    }
}
