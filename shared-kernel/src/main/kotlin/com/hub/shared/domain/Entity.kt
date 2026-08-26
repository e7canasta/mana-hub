package com.hub.shared.domain

public abstract class Entity<T : Any> {
    public abstract val id: T

    private val domainEvents = mutableListOf<DomainEvent>()

    protected fun registerEvent(event: DomainEvent) {
        domainEvents.add(event)
    }

    public fun pullEvents(): List<DomainEvent> {
        val events = domainEvents.toList()
        domainEvents.clear()
        return events
    }

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Entity<*>) return false
        return id == other.id
    }

    public override fun hashCode(): Int = id.hashCode()
}

public abstract class AggregateRoot<T : Any> : Entity<T>() {
    public open var version: Long = 0L
        protected set
}
