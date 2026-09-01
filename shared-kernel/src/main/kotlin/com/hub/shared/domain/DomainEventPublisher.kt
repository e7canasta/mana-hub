package com.hub.shared.domain

import kotlin.reflect.KClass

public interface DomainEventPublisher {
    public fun publish(event: DomainEvent)
    public fun publishAll(events: List<DomainEvent>)
}

public inline fun <reified T : DomainEvent> DomainEventPublisher.publishTyped(event: T) {
    publish(event)
}

fun DomainEventPublisher.publishAndClear(aggregate: AggregateRoot<*>) {
    aggregate.domainEvents.forEach { publish(it) }
    aggregate.clearEvents()
}
