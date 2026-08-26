package com.hub.care.domain.model

import java.util.UUID

@JvmInline
value class RoundId(val value: String) {
    companion object {
        fun from(value: String): RoundId = RoundId(value)
        fun random(): RoundId = RoundId(UUID.randomUUID().toString())
    }
}
