package com.hub.care.domain.model

enum class RoundTaskStatus {
    PENDING, IN_PROGRESS, COMPLETED;

    companion object {
        fun from(value: String): RoundTaskStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown round task status: $value")
    }
}
