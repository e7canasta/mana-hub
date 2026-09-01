package com.hub.care.domain.model

import com.hub.shared.domain.AggregateRoot
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.BedId
import java.time.Instant

class RoundTask private constructor(
    override val id: RoundTaskId,
    val roundId: RoundId,
    val residentId: ResidentId,
    val bedId: BedId?,
    val status: RoundTaskStatus,
    val note: String?,
    val completedAt: Instant?,
    val completedBy: String?,
    override var version: Long
) : AggregateRoot<RoundTaskId>() {

    fun complete(note: String?, completedBy: String): RoundTask {
        require(status == RoundTaskStatus.PENDING) { "Task is not pending" }
        return reconstitute(
            id = id, roundId = roundId, residentId = residentId, bedId = bedId,
            status = RoundTaskStatus.COMPLETED, note = note, completedAt = Instant.now(),
            completedBy = completedBy, version = version + 1
        )
    }

    companion object {
        fun create(roundId: RoundId, residentId: ResidentId, bedId: BedId?): RoundTask = RoundTask(
            id = RoundTaskId.random(), roundId = roundId, residentId = residentId, bedId = bedId,
            status = RoundTaskStatus.PENDING, note = null, completedAt = null,
            completedBy = null, version = 0
        )

        fun reconstitute(
            id: RoundTaskId, roundId: RoundId, residentId: ResidentId, bedId: BedId?,
            status: RoundTaskStatus, note: String?, completedAt: Instant?,
            completedBy: String?, version: Long
        ): RoundTask = RoundTask(id, roundId, residentId, bedId, status, note, completedAt, completedBy, version)
    }
}
