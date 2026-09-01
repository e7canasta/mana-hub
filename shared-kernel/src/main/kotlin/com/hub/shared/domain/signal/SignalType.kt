package com.hub.shared.domain.signal

/**
 * Sentinel signal types arriving from mana-hive.
 * Exhaustive matching at compile-time — missing a branch is a compiler error.
 */
enum class SignalType {
    EPISODE_OPENED,
    EPISODE_CLOSED,
    AUTO_RECOVERY,
    EPISODE_COMPLICATED,
    UMBRELLA_EVENT,
    SUPPRESSED_WITH_RECORD,
    DWELL_PRE_WARNING,
    COME_BACK_PRE_WARNING,
    ;

    companion object {
        fun from(value: String): SignalType? =
            entries.find { it.name == value }
    }
}
