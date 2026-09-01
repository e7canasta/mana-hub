package com.hub.care.domain.event

import com.hub.shared.domain.DomainEvent
import com.hub.shared.domain.WingId
import com.hub.care.domain.model.RoundId
import java.time.Instant
import java.util.UUID

sealed interface RoundEvent : DomainEvent {

    data class Started(
        override val eventId: String = UUID.randomUUID().toString(),
        override val occurredAt: Instant = Instant.now(),
        val roundId: RoundId,
        val wingId: WingId,
        val actorId: String,
    ) : RoundEvent {
        override val eventType: String = "RoundStarted"
    }

    data class Completed(
        override val eventId: String = UUID.randomUUID().toString(),
        override val occurredAt: Instant = Instant.now(),
        val roundId: RoundId,
        val wingId: WingId,
        val actorId: String,
    ) : RoundEvent {
        override val eventType: String = "RoundCompleted"
    }

    data class Cancelled(
        override val eventId: String = UUID.randomUUID().toString(),
        override val occurredAt: Instant = Instant.now(),
        val roundId: RoundId,
        val wingId: WingId,
    ) : RoundEvent {
        override val eventType: String = "RoundCancelled"
    }
}
