package com.hub.identity.domain.model

import java.util.UUID

@JvmInline
value class UserId(val value: String) {
    companion object {
        fun from(value: String): UserId = UserId(value)
        fun random(): UserId = UserId(UUID.randomUUID().toString())
    }
}
