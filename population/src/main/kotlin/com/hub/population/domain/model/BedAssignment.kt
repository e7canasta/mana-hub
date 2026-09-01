package com.hub.population.domain.model

import com.hub.shared.domain.AggregateRoot
import com.hub.shared.domain.BedId
import com.hub.shared.domain.ResidentId
import com.hub.population.domain.event.BedAssignmentEvent
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

    private val _domainEvents = mutableListOf<BedAssignmentEvent>()
    val domainEvents: List<BedAssignmentEvent> get() = _domainEvents.toList()
    fun clearEvents() = _domainEvents.clear()

    val isOpen: Boolean get() = endsAt == null

    fun close(): BedAssignment {
        require(isOpen) { "Assignment is already closed" }
        val next = reconstitute(
            id = id, residentId = residentId, bedId = bedId, startsAt = startsAt,
            endsAt = Instant.now(), createdBy = createdBy, version = version + 1
        )
        next._domainEvents.add(
            BedAssignmentEvent.Closed(assignmentId = id, residentId = residentId, bedId = bedId)
        )
        return next
    }

    companion object {
        fun create(residentId: ResidentId, bedId: BedId, createdBy: String?): BedAssignment {
            val assignment = BedAssignment(
                id = AssignmentId.random(), residentId = residentId, bedId = bedId,
                startsAt = Instant.now(), endsAt = null, createdBy = createdBy, version = 0
            )
            assignment._domainEvents.add(
                BedAssignmentEvent.Created(
                    assignmentId = assignment.id, residentId = residentId,
                    bedId = bedId, createdBy = createdBy,
                )
            )
            return assignment
        }

        fun reconstitute(
            id: AssignmentId, residentId: ResidentId, bedId: BedId, startsAt: Instant,
            endsAt: Instant?, createdBy: String?, version: Long
        ): BedAssignment = BedAssignment(id, residentId, bedId, startsAt, endsAt, createdBy, version)
    }
}
