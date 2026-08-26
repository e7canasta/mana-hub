package com.hub.shared.domain

import java.time.Instant
import java.util.UUID

public interface DomainEvent {
    public val eventId: String get() = UUID.randomUUID().toString()
    public val occurredAt: Instant get() = Instant.now()
    public val eventType: String
}
