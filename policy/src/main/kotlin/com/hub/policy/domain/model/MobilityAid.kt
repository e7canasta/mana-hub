package com.hub.policy.domain.model

enum class MobilityAid {
    NONE,
    WALKER,
    WHEELCHAIR;

    companion object {
        fun from(value: String): MobilityAid = when (value.lowercase()) {
            "none", "" -> NONE
            "walker" -> WALKER
            "wheelchair" -> WHEELCHAIR
            else -> throw IllegalArgumentException("Unknown mobility aid: $value")
        }
    }
}
