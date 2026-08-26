package com.hub.policy.domain.model

import java.util.UUID

@JvmInline
value class AlarmProfileId(val value: String) {
    companion object {
        fun from(value: String): AlarmProfileId = AlarmProfileId(value)
        fun random(): AlarmProfileId = AlarmProfileId(UUID.randomUUID().toString())
    }
}
