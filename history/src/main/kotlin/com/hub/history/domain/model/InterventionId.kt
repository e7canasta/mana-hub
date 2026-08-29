package com.hub.history.domain.model

import java.util.UUID

@JvmInline
value class InterventionId(val value: String) {
    companion object {
        fun from(value: String): InterventionId = InterventionId(value)
        fun random(): InterventionId = InterventionId(UUID.randomUUID().toString())
    }
}
