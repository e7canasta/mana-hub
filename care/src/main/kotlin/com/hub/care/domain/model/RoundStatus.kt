package com.hub.care.domain.model

enum class RoundStatus {
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    companion object {
        fun from(value: String): RoundStatus = when (value.lowercase()) {
            "in_progress" -> IN_PROGRESS
            "completed" -> COMPLETED
            "cancelled" -> CANCELLED
            else -> throw IllegalArgumentException("Unknown status: $value")
        }
    }
}
