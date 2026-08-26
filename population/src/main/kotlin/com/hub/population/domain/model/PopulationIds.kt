package com.hub.population.domain.model

import java.util.UUID

@JvmInline
value class ResidentId(val value: String) {
    companion object {
        fun from(value: String): ResidentId = ResidentId(value)
        fun random(): ResidentId = ResidentId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class AssignmentId(val value: String) {
    companion object {
        fun from(value: String): AssignmentId = AssignmentId(value)
        fun random(): AssignmentId = AssignmentId(UUID.randomUUID().toString())
    }
}
