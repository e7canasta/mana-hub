package com.hub.history.domain.model

enum class EventSource {
    SENTINEL,
    CAMERA,
    SENSOR,
    STAFF,
    MANUAL,
    OTHER;

    companion object {
        fun from(value: String): EventSource = when (value.uppercase()) {
            "SENTINEL" -> SENTINEL
            "CAMERA" -> CAMERA
            "SENSOR" -> SENSOR
            "STAFF" -> STAFF
            "MANUAL" -> MANUAL
            else -> OTHER
        }
    }
}
