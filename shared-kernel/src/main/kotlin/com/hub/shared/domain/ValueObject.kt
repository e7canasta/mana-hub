package com.hub.shared.domain

import java.time.Instant

public abstract class ValueObject {
    public abstract fun copy(): Any
}

public data class InstantValue(public val value: Instant) : ValueObject() {
    public override fun copy(): InstantValue = this

    public companion object {
        public fun now(): InstantValue = InstantValue(Instant.now())
        public fun of(instant: Instant): InstantValue = InstantValue(instant)
    }
}
