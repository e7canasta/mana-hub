package com.hub.care.domain.model

import java.util.UUID

@JvmInline
value class RoundTaskId(val value: String) {
    companion object {
        fun from(value: String): RoundTaskId = RoundTaskId(value)
        fun random(): RoundTaskId = RoundTaskId(UUID.randomUUID().toString())
    }
}
