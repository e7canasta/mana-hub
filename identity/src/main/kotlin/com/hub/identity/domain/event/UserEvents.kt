package com.hub.identity.domain.event

import com.hub.shared.domain.DomainEvent
import com.hub.shared.domain.UserId

data class UserCreated(
    val userId: UserId,
    val username: String
) : DomainEvent {
    override val eventType: String = "identity.user.created"
}

data class UserRetired(
    val id: UserId,
    val actorId: UserId
) : DomainEvent {
    override val eventType: String = "identity.user.retired"
}
