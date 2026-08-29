package com.hub.shared.domain

public abstract class Entity<T : Any> {
    public abstract val id: T

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
