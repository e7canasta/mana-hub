package com.hub.clients.observation

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import java.time.Instant
import java.time.LocalDate

// ══════════════════════════════════════════════════════════════
//  PERCEPTION KIND — Vocabulary: §3.1
// ══════════════════════════════════════════════════════════════

enum class PerceptionKind(@JsonValue val apiValue: String) {
    POSTURE("POSTURE"),
    LOCATION("LOCATION"),
    STAFF_PRESENCE("STAFF_PRESENCE"),
    ACCESSORY_PRESENCE("ACCESSORY_PRESENCE")
}

// ══════════════════════════════════════════════════════════════
//  SCENE CHANGE KIND — Vocabulary: §3.2
// ══════════════════════════════════════════════════════════════

enum class SceneChangeKind(@JsonValue val apiValue: String) {
    TRANSITION("TRANSITION"),
    PERMANENCE("PERMANENCE")
}

enum class TriggerType(@JsonValue val apiValue: String) {
    HYSTERESIS("hysteresis"),
    PERMANENCE("permanence"),
    MANUAL("manual")
}

data class BedStateResponse(
    @JsonProperty("bedId") val bedId: String,
    @JsonProperty("residentId") val residentId: String? = null,
    @JsonProperty("roomState") val roomState: String? = null,
    val state: String? = null,
    val sleeping: Boolean? = null,
    @JsonProperty("stateSince") val stateSince: Instant,
    @JsonProperty("staffPresent") val staffPresent: Boolean? = null
)

data class SleepSummaryResponse(
    @JsonProperty("residentId") val residentId: String,
    @JsonProperty("observedOn") val observedOn: LocalDate,
    @JsonProperty("calmMinutes") val calmMinutes: Int,
    @JsonProperty("restlessMinutes") val restlessMinutes: Int,
    @JsonProperty("awakeMinutes") val awakeMinutes: Int,
    @JsonProperty("outOfBedMinutes") val outOfBedMinutes: Int
)

data class MobilitySummaryResponse(
    @JsonProperty("residentId") val residentId: String,
    @JsonProperty("observedOn") val observedOn: LocalDate,
    @JsonProperty("walkingMinutes") val walkingMinutes: Int,
    @JsonProperty("distanceMeters") val distanceMeters: Double,
    @JsonProperty("transferCount") val transferCount: Int
)

data class BathroomSummaryResponse(
    @JsonProperty("residentId") val residentId: String,
    @JsonProperty("observedOn") val observedOn: LocalDate,
    @JsonProperty("visitCount") val visitCount: Int,
    @JsonProperty("nightVisitCount") val nightVisitCount: Int
)

data class IngestEventRequest(
    @JsonProperty("sourceEventId") val sourceEventId: String,
    @JsonProperty("monitorKey") val monitorKey: String,
    @JsonProperty("bedId") val bedId: String? = null,
    @JsonProperty("residentId") val residentId: String? = null,
    val kind: String,
    @JsonProperty("roomState") val roomState: String? = null,
    val state: String? = null,
    val sleeping: Boolean? = null,
    @JsonProperty("occurredAt") val occurredAt: Instant,
    @JsonProperty("payloadJson") val payloadJson: String = "{}"
)

data class IngestSummaryRequest<T>(
    @JsonProperty("sourceRecordId") val sourceRecordId: String,
    @JsonProperty("residentId") val residentId: String,
    @JsonProperty("observedOn") val observedOn: LocalDate,
    val data: T,
    val source: String? = null,
    @JsonProperty("modelVersion") val modelVersion: String? = null,
    val confidence: Double? = null
)

data class SleepSummaryData(
    @JsonProperty("calmMinutes") val calmMinutes: Int = 0,
    @JsonProperty("restlessMinutes") val restlessMinutes: Int = 0,
    @JsonProperty("awakeMinutes") val awakeMinutes: Int = 0,
    @JsonProperty("outOfBedMinutes") val outOfBedMinutes: Int = 0,
    @JsonProperty("bedExitCount") val bedExitCount: Int = 0,
    @JsonProperty("wakeCount") val wakeCount: Int = 0
)

data class MobilitySummaryData(
    @JsonProperty("inBedMinutes") val inBedMinutes: Int = 0,
    @JsonProperty("outOfBedMinutes") val outOfBedMinutes: Int = 0,
    @JsonProperty("outOfSightMinutes") val outOfSightMinutes: Int = 0,
    @JsonProperty("walkingMinutes") val walkingMinutes: Int = 0,
    @JsonProperty("distanceMeters") val distanceMeters: Double = 0.0,
    @JsonProperty("transferCount") val transferCount: Int = 0
)

data class BathroomSummaryData(
    @JsonProperty("visitCount") val visitCount: Int = 0,
    @JsonProperty("nightVisitCount") val nightVisitCount: Int = 0,
    @JsonProperty("assistedCount") val assistedCount: Int = 0,
    @JsonProperty("totalMinutes") val totalMinutes: Int = 0
)

data class IngestNotificationRequest(
    @JsonProperty("category") val category: String,
    @JsonProperty("bedId") val bedId: String? = null,
    @JsonProperty("residentId") val residentId: String? = null,
    @JsonProperty("eventType") val eventType: String,
    @JsonProperty("timestamp") val timestamp: Instant,
    @JsonProperty("ruleId") val ruleId: String? = null,
    @JsonProperty("riskLevel") val riskLevel: String? = null,
    @JsonProperty("payloadJson") val payloadJson: String = "{}"
)

data class NotificationResponse(
    val id: String,
    val category: String,
    @JsonProperty("bedId") val bedId: String? = null,
    @JsonProperty("residentId") val residentId: String? = null,
    @JsonProperty("eventType") val eventType: String,
    @JsonProperty("timestamp") val timestamp: Instant,
    @JsonProperty("ruleId") val ruleId: String? = null,
    @JsonProperty("riskLevel") val riskLevel: String? = null
)

// ══════════════════════════════════════════════════════════════
//  PERCEPTION — Raw sensor reading at an instant
//  Vocabulary: docs/vocabulario-unificado.md §3.1
// ══════════════════════════════════════════════════════════════

data class IngestPerceptionRequest(
    @JsonProperty("sourceEventId") val sourceEventId: String,
    @JsonProperty("monitorKey") val monitorKey: String,
    @JsonProperty("bedId") val bedId: String? = null,
    @JsonProperty("residentId") val residentId: String? = null,
    val kind: PerceptionKind,
    @JsonProperty("roomState") val roomState: String? = null,
    val state: String? = null,
    val sleeping: Boolean? = null,
    @JsonProperty("occurredAt") val occurredAt: Instant,
    @JsonProperty("payloadJson") val payloadJson: String = "{}"
)

data class PerceptionResponse(
    val id: String,
    @JsonProperty("monitorKey") val monitorKey: String,
    @JsonProperty("bedId") val bedId: String? = null,
    @JsonProperty("residentId") val residentId: String? = null,
    val kind: PerceptionKind,
    val state: String? = null,
    val sleeping: Boolean? = null,
    @JsonProperty("occurredAt") val occurredAt: Instant
)

// ══════════════════════════════════════════════════════════════
//  SCENE CHANGE — Confirmed state transition after hysteresis
//  Vocabulary: docs/vocabulario-unificado.md §3.2
// ══════════════════════════════════════════════════════════════

data class SceneChangeRequest(
    @JsonProperty("sourceEventId") val sourceEventId: String,
    @JsonProperty("bedId") val bedId: String,
    @JsonProperty("residentId") val residentId: String? = null,
    @JsonProperty("eventType") val eventType: SceneChangeKind,
    @JsonProperty("fromState") val fromState: String,
    @JsonProperty("toState") val toState: String,
    @JsonProperty("triggerType") val triggerType: TriggerType = TriggerType.HYSTERESIS,
    @JsonProperty("occurredAt") val occurredAt: Instant,
    @JsonProperty("payloadJson") val payloadJson: String = "{}"
)

data class SceneChangeResponse(
    val id: String,
    @JsonProperty("eventId") val eventId: String,
    @JsonProperty("bedId") val bedId: String,
    @JsonProperty("residentId") val residentId: String? = null,
    @JsonProperty("eventType") val eventType: SceneChangeKind,
    @JsonProperty("fromState") val fromState: String,
    @JsonProperty("toState") val toState: String,
    @JsonProperty("triggerType") val triggerType: TriggerType,
    @JsonProperty("occurredAt") val occurredAt: Instant
)
