package com.hub.observation.domain.model

enum class SensorEventKind {
    PIR, CAMERA, RADAR, BLUETOOTH, MANUAL, SCHEDULED;

    companion object {
        fun from(value: String): SensorEventKind =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: value.uppercase().replace("-", "_").let { name ->
                    entries.firstOrNull { it.name == name } ?: MANUAL
                }
    }
}
