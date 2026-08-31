package com.hub.surveillance.domain.event

import com.hub.shared.domain.DomainEvent
import com.hub.surveillance.domain.model.EpisodeId
import com.hub.surveillance.domain.model.EpisodeSeverity
import java.time.Instant
import java.util.UUID

sealed interface EpisodeEvent : DomainEvent {

    data class Created(
        override val eventId: String = UUID.randomUUID().toString(),
        override val occurredAt: Instant = Instant.now(),
        val episodeId: EpisodeId,
        val residentId: String,
        val bedId: String?,
        val severity: EpisodeSeverity,
        val title: String?,
    ) : EpisodeEvent {
        override val eventType: String = "EpisodeCreated"
    }

    data class Acknowledged(
        override val eventId: String = UUID.randomUUID().toString(),
        override val occurredAt: Instant = Instant.now(),
        val episodeId: EpisodeId,
        val actorId: String,
    ) : EpisodeEvent {
        override val eventType: String = "EpisodeAcknowledged"
    }

    data class Resolved(
        override val eventId: String = UUID.randomUUID().toString(),
        override val occurredAt: Instant = Instant.now(),
        val episodeId: EpisodeId,
        val actorId: String,
    ) : EpisodeEvent {
        override val eventType: String = "EpisodeResolved"
    }

    data class Escalated(
        override val eventId: String = UUID.randomUUID().toString(),
        override val occurredAt: Instant = Instant.now(),
        val episodeId: EpisodeId,
        val targetId: String,
        val newLevel: Int,
    ) : EpisodeEvent {
        override val eventType: String = "EpisodeEscalated"
    }
}
