package com.hub.identity.domain.event

import com.hub.shared.domain.DomainEvent

data class UserCreated(
    val userId: com.hub.identity.domain.model.UserId,
    val username: String
) : DomainEvent {
    override val eventType: String = "identity.user.created"
}

data class UserRetired(
    val id: com.hub.identity.domain.model.UserId,
    val actorId: com.hub.identity.domain.model.UserId
) : DomainEvent {
    override val eventType: String = "identity.user.retired"
}
