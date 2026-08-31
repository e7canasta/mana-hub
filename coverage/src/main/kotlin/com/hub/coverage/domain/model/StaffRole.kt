package com.hub.coverage.domain.model

enum class StaffRole {
    NURSE,
    DOCTOR,
    CAREGIVER,
    PHYSIOTHERAPIST,
    SOCIAL_WORKER,
    ADMINISTRATOR,
    OTHER;

    companion object {
        fun from(value: String): StaffRole = when (value.uppercase()) {
            "NURSE" -> NURSE
            "DOCTOR" -> DOCTOR
            "CAREGIVER" -> CAREGIVER
            "PHYSIOTHERAPIST" -> PHYSIOTHERAPIST
            "SOCIAL_WORKER" -> SOCIAL_WORKER
            "ADMINISTRATOR" -> ADMINISTRATOR
            else -> OTHER
        }
    }
}
