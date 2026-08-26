package com.hub.identity.domain.model

enum class Role {
    OWNER,
    SUPERVISOR,
    STAFF;

    companion object {
        fun from(value: String): Role = when (value.lowercase()) {
            "owner" -> OWNER
            "supervisor" -> SUPERVISOR
            "staff" -> STAFF
            else -> throw IllegalArgumentException("Unknown role: $value")
        }
    }
}
