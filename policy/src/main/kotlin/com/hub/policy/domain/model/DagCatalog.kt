package com.hub.policy.domain.model

import java.time.Duration

/**
 * Resident states that can be monitored.
 */
enum class StateKind {
    LYING,
    SITTING_IN_BED,
    BED_EDGE,
    STANDING,
    IN_BATHROOM,
    IN_ROOM,
    ABSENT;

    companion object {
        fun from(value: String): StateKind = when (value.lowercase()) {
            "lying" -> LYING
            "sitting", "sitting_in_bed" -> SITTING_IN_BED
            "bed_edge" -> BED_EDGE
            "standing" -> STANDING
            "bathroom", "in_bathroom" -> IN_BATHROOM
            "room", "in_room" -> IN_ROOM
            "absent" -> ABSENT
            else -> throw IllegalArgumentException("Unknown state kind: $value")
        }
    }
}

/**
 * Severity levels for alerts.
 */
enum class Severity {
    INFO,
    WARNING,
    HIGH,
    CRITICAL
}

/**
 * How an episode is closed.
 */
enum class ClosureCondition {
    SAFE_ONLY,
    STAFF_ONLY,
    STAFF_OR_SAFE,
    STAFF_AND_SAFE
}

/**
 * How an alert is triggered.
 */
enum class TriggerOn {
    ENTRY,
    DWELL,
    COME_BACK
}

/**
 * Rule for a specific resident state: when to warn, when to alert, severity, closure.
 */
data class ResidentStateRule(
    val state: StateKind,
    val warningAfter: Duration? = null,
    val alertAfter: Duration? = null,
    val alertOnEntry: Boolean = false,
    val severity: Severity = Severity.WARNING,
    val closureCondition: ClosureCondition = ClosureCondition.STAFF_OR_SAFE,
) {
    val alerts: Boolean get() = alertOnEntry || alertAfter != null
    val triggerOn: TriggerOn get() = if (alertOnEntry) TriggerOn.ENTRY else TriggerOn.DWELL
}

/**
 * Rule for returning to a baseline state (come-back).
 */
data class ComeBackRule(
    val baseline: StateKind,
    val warningAfter: Duration? = null,
    val alertAfter: Duration? = null,
    val severity: Severity = Severity.WARNING,
    val closureCondition: ClosureCondition = ClosureCondition.STAFF_OR_SAFE,
) {
    val alerts: Boolean get() = alertAfter != null
}

/**
 * Transition between two states with hysteresis and optional recording window.
 */
data class DagTransitionRule(
    val from: StateKind,
    val to: StateKind,
    val hysteresis: Duration = Duration.ofSeconds(1),
    val recordBefore: Duration? = null,
    val recordAfter: Duration? = null,
)

/**
 * The DAG catalog: defines all monitoring rules for a watch level.
 */
data class DagCatalog(
    val version: String,
    val residentStates: Map<StateKind, ResidentStateRule>,
    val comeBackRules: Map<StateKind, ComeBackRule>,
    val transitions: List<DagTransitionRule>,
)
