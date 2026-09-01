package com.hub.population.domain.event

import com.hub.shared.domain.BedId
import com.hub.shared.domain.DomainEvent
import com.hub.shared.domain.ResidentId
import com.hub.population.domain.model.AssignmentId
import java.time.Instant
import java.util.UUID

sealed interface BedAssignmentEvent : DomainEvent {

    data class Created(
        override val eventId: String = UUID.randomUUID().toString(),
        override val occurredAt: Instant = Instant.now(),
        val assignmentId: AssignmentId,
        val residentId: ResidentId,
        val bedId: BedId,
        val createdBy: String?,
    ) : BedAssignmentEvent {
        override val eventType: String = "BedAssignmentCreated"
    }

    data class Closed(
        override val eventId: String = UUID.randomUUID().toString(),
        override val occurredAt: Instant = Instant.now(),
        val assignmentId: AssignmentId,
        val residentId: ResidentId,
        val bedId: BedId,
    ) : BedAssignmentEvent {
        override val eventType: String = "BedAssignmentClosed"
    }
}
