package com.hub.shared.domain

import java.util.UUID

@JvmInline
public value class Identifier(public val value: String = UUID.randomUUID().toString()) {
    init {
        require(value.isNotBlank()) { "Identifier must not be blank" }
    }

    public companion object {
        public fun from(value: String): Identifier = Identifier(value)
        public fun random(): Identifier = Identifier()
    }

    public override fun toString(): String = value
}
