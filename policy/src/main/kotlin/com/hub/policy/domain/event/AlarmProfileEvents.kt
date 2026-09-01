package com.hub.policy.domain.event

import com.hub.shared.domain.DomainEvent
import com.hub.shared.domain.ResidentId
import com.hub.policy.domain.model.AlarmProfileId
import com.hub.policy.domain.model.RiskLevel
import java.time.Instant
import java.util.UUID

sealed interface AlarmProfileEvent : DomainEvent {

    data class Created(
        override val eventId: String = UUID.randomUUID().toString(),
        override val occurredAt: Instant = Instant.now(),
        val profileId: AlarmProfileId,
        val residentId: ResidentId,
        val updatedBy: String?,
    ) : AlarmProfileEvent {
        override val eventType: String = "AlarmProfileCreated"
    }

    data class Updated(
        override val eventId: String = UUID.randomUUID().toString(),
        override val occurredAt: Instant = Instant.now(),
        val profileId: AlarmProfileId,
        val residentId: ResidentId,
        val riskLevel: RiskLevel,
        val updatedBy: String?,
    ) : AlarmProfileEvent {
        override val eventType: String = "AlarmProfileUpdated"
    }

    data class Expired(
        override val eventId: String = UUID.randomUUID().toString(),
        override val occurredAt: Instant = Instant.now(),
        val profileId: AlarmProfileId,
        val residentId: ResidentId,
    ) : AlarmProfileEvent {
        override val eventType: String = "AlarmProfileExpired"
    }
}
