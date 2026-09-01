package com.hub.population.domain.event

import com.hub.shared.domain.DomainEvent
import com.hub.shared.domain.ResidentId
import com.hub.population.domain.model.ResidentStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

sealed interface ResidentEvent : DomainEvent {

    data class Admitted(
        override val eventId: String = UUID.randomUUID().toString(),
        override val occurredAt: Instant = Instant.now(),
        val residentId: ResidentId,
        val fullName: String,
        val admissionDate: LocalDate,
        val birthDate: LocalDate?,
        val externalId: String?,
    ) : ResidentEvent {
        override val eventType: String = "ResidentAdmitted"
    }

    data class Discharged(
        override val eventId: String = UUID.randomUUID().toString(),
        override val occurredAt: Instant = Instant.now(),
        val residentId: ResidentId,
        val actorId: String,
    ) : ResidentEvent {
        override val eventType: String = "ResidentDischarged"
    }

    data class Updated(
        override val eventId: String = UUID.randomUUID().toString(),
        override val occurredAt: Instant = Instant.now(),
        val residentId: ResidentId,
        val fullName: String,
        val birthDate: LocalDate?,
    ) : ResidentEvent {
        override val eventType: String = "ResidentUpdated"
    }
}
