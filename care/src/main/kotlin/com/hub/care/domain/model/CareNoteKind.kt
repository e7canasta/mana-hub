package com.hub.care.domain.model

enum class CareNoteKind {
    GENERAL, CLINICAL, CARE, INSIGHT, PATTERN, OBSERVATION, SUMMARY;

    companion object {
        fun from(value: String): CareNoteKind =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown care note kind: $value")
    }
}
