package com.hub.blueprints.supporting

/**
 * Canonical vocabulary — mana-hub
 *
 * Event chain:
 *   PERCEPTION → SCENE CHANGE → EPISODE → CLINICAL HISTORY
 *
 * Reference: docs/vocabulario-unificado.md
 */

// ══════════════════════════════════════════════════════════════
//  PERCEPTION — Raw sensor reading at an instant
// ══════════════════════════════════════════════════════════════

enum class PerceptionKind {
    POSTURE,
    LOCATION,
    STAFF_PRESENCE,
    ACCESSORY_PRESENCE
}

// ══════════════════════════════════════════════════════════════
//  SCENE CHANGE — Confirmed transition after hysteresis
// ══════════════════════════════════════════════════════════════

enum class SceneChangeKind {
    TRANSITION,
    PERMANENCE
}

// ══════════════════════════════════════════════════════════════
//  EPISODE — Event requiring attention
// ══════════════════════════════════════════════════════════════

enum class EpisodeSeverityKind {
    INFO,
    WARNING,
    CRITICAL,
    EMERGENCY
}

enum class EpisodeStatusKind {
    PENDING,
    ACKNOWLEDGED,
    RESOLVED,
    AUTO_RESOLVED
}

// ══════════════════════════════════════════════════════════════
//  FINDING — Clinical insight/conclusion
// ══════════════════════════════════════════════════════════════

enum class FindingKind {
    INSIGHT,
    PATTERN,
    OBSERVATION,
    RECOMMENDATION
}

// ══════════════════════════════════════════════════════════════
//  NOTES — Note types by context
// ══════════════════════════════════════════════════════════════

enum class ResidentNoteKind {
    CARE,
    CLINICAL,
    INSIGHT,
    PATTERN,
    OBSERVATION,
    SUMMARY
}

enum class EpisodeNoteKind {
    ACKNOWLEDGEMENT,
    RESOLUTION,
    CLINICAL_NOTE
}

enum class ShiftNoteKind {
    SHIFT_SUMMARY,
    INCIDENT_REPORT,
    GENERAL
}

// ══════════════════════════════════════════════════════════════
//  RISK — Monitoring profile risk level
// ══════════════════════════════════════════════════════════════

enum class RiskLevelKind {
    LOW,
    MEDIUM,
    HIGH
}

// ══════════════════════════════════════════════════════════════
//  MAPPING: FindingKind → ResidentNoteKind
// ══════════════════════════════════════════════════════════════

fun FindingKind.toResidentNoteKind(): ResidentNoteKind = when (this) {
    FindingKind.INSIGHT -> ResidentNoteKind.INSIGHT
    FindingKind.PATTERN -> ResidentNoteKind.PATTERN
    FindingKind.OBSERVATION -> ResidentNoteKind.OBSERVATION
    FindingKind.RECOMMENDATION -> ResidentNoteKind.INSIGHT
}
