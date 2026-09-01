package com.hub.observation.domain.model

enum class NotificationCategory {
    INFORMATIONAL, WARNING, CRITICAL, EMERGENCY;

    companion object {
        fun from(value: String): NotificationCategory =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: value.uppercase().replace("-", "_").let { name ->
                    entries.firstOrNull { it.name == name } ?: INFORMATIONAL
                }
    }
}
