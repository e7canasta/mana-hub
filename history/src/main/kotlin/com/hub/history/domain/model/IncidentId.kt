package com.hub.history.domain.model

import java.util.UUID

@JvmInline
value class IncidentId(val value: String) {
    companion object {
        fun from(value: String): IncidentId = IncidentId(value)
        fun random(): IncidentId = IncidentId(UUID.randomUUID().toString())
    }
}
