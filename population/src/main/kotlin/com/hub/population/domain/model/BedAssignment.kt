package com.hub.population.domain.model

import com.hub.shared.domain.AggregateRoot
import com.hub.shared.domain.BedId
import com.hub.shared.domain.ResidentId
import java.time.Instant

class BedAssignment private constructor(
    override val id: AssignmentId,
    val residentId: ResidentId,
    val bedId: BedId,
    val startsAt: Instant,
    val endsAt: Instant?,
    val createdBy: String?,
    override var version: Long
) : AggregateRoot<AssignmentId>() {

    val isOpen: Boolean get() = endsAt == null

    fun close(): BedAssignment {
        require(isOpen) { "Assignment is already closed" }
        return reconstitute(
            id = id, residentId = residentId, bedId = bedId, startsAt = startsAt,
            endsAt = Instant.now(), createdBy = createdBy, version = version + 1
        )
    }

    companion object {
        fun create(residentId: ResidentId, bedId: BedId, createdBy: String?): BedAssignment = BedAssignment(
            id = AssignmentId.random(), residentId = residentId, bedId = bedId,
            startsAt = Instant.now(), endsAt = null, createdBy = createdBy, version = 0
        )

        fun reconstitute(
            id: AssignmentId, residentId: ResidentId, bedId: BedId, startsAt: Instant,
            endsAt: Instant?, createdBy: String?, version: Long
        ): BedAssignment = BedAssignment(id, residentId, bedId, startsAt, endsAt, createdBy, version)
    }
}
