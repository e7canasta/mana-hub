package com.hub.care.domain.model

import com.hub.shared.domain.AggregateRoot
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.BedId
import java.time.Instant

class RoundTask private constructor(
    override val id: RoundId,
    val roundId: RoundId,
    val residentId: ResidentId,
    val bedId: BedId?,
    val status: String,
    val note: String?,
    val completedAt: Instant?,
    val completedBy: String?,
    override var version: Long
) : AggregateRoot<RoundId>() {

    val isPending: Boolean get() = status == "pending"
    val isCompleted: Boolean get() = status == "completed"

    fun complete(note: String?, completedBy: String): RoundTask {
        require(isPending) { "Task is not pending" }
        return reconstitute(
            id = id, roundId = roundId, residentId = residentId, bedId = bedId,
            status = "completed", note = note, completedAt = Instant.now(),
            completedBy = completedBy, version = version + 1
        )
    }

    companion object {
        fun create(roundId: RoundId, residentId: ResidentId, bedId: BedId?): RoundTask = RoundTask(
            id = RoundId.random(), roundId = roundId, residentId = residentId, bedId = bedId,
            status = "pending", note = null, completedAt = null, completedBy = null, version = 0
        )

        fun reconstitute(
            id: RoundId, roundId: RoundId, residentId: ResidentId, bedId: BedId?,
            status: String, note: String?, completedAt: Instant?, completedBy: String?, version: Long
        ): RoundTask = RoundTask(id, roundId, residentId, bedId, status, note, completedAt, completedBy, version)
    }
}
