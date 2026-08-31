package com.hub.insights.inbound

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

data class HubResident(
    val id: String,
    val fullName: String? = null,
    val admissionDate: LocalDate? = null,
    val status: String? = null,
)

data class HubChart(
    val id: String,
    val fullName: String? = null,
    val admissionDate: LocalDate? = null,
)

/**
 * Fila que devuelve hub (`GET .../scene-events`) **o** el JSON hive original.
 *
 * Hive ([SceneEvent.TransitionDetected]): `type`, `at`, `from`, `to` = simpleName de PersonState.
 * Hive ([SceneEvent.NightOpened]): `initialState` (no `to`).
 * Hub aplana: `eventType`, `timestamp`, `fromState`, `toState` (`initialState` → `toState`).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class HubSceneEvent(
    val id: String? = null,
    val eventId: String? = null,
    val bedId: String? = null,
    @JsonProperty("bed") val bed: String? = null,
    val residentId: String? = null,
    val eventType: String? = null,
    val type: String? = null,
    val fromState: String? = null,
    val toState: String? = null,
    val from: String? = null,
    val to: String? = null,
    val initialState: String? = null,
    val triggerType: String? = null,
    val timestamp: Instant? = null,
    val occurredAt: Instant? = null,
    val at: Instant? = null,
    val twinSnapshot: HubTwinSnapshot? = null,
) {
    fun occurredAtInstant(): Instant? = at ?: timestamp ?: occurredAt

    fun typeName(): String? = type ?: eventType

    fun fromName(): String? = from ?: fromState

    fun toName(): String? = to ?: toState ?: initialState ?: twinSnapshot?.state
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class HubTwinSnapshot(
    val state: String? = null,
)

data class HubSleepDay(
    val day: String,
    val calmMinutes: Int = 0,
    val restlessMinutes: Int = 0,
    val awakeMinutes: Int = 0,
    val outOfBedMinutes: Int = 0,
    val bedExitCount: Int = 0,
    val wakeCount: Int = 0,
    val startedAt: LocalDateTime? = null,
    val endedAt: LocalDateTime? = null,
)

data class HubSleepTab(
    val residentId: String,
    val from: String? = null,
    val to: String? = null,
    val observedFrom: String? = null,
    val summaries: List<HubSleepDay> = emptyList(),
)

data class HubMobilityDay(
    val day: String,
    val walkingMinutes: Int = 0,
    val distanceMeters: Double = 0.0,
    val transferCount: Int = 0,
    val outOfBedMinutes: Int = 0,
)

data class HubMobilityTab(
    val residentId: String,
    val from: String? = null,
    val to: String? = null,
    val observedFrom: String? = null,
    val summaries: List<HubMobilityDay> = emptyList(),
)

data class HubBathroomDay(
    val day: String,
    val visitCount: Int = 0,
    val nightVisitCount: Int = 0,
)

data class HubBathroomTab(
    val residentId: String,
    val from: String? = null,
    val to: String? = null,
    val observedFrom: String? = null,
    val summaries: List<HubBathroomDay> = emptyList(),
)

data class HubCareDay(
    val day: String,
    val totalMinutes: Int = 0,
    val proactiveMinutes: Int = 0,
    val roundsCount: Int = 0,
    val notesCount: Int = 0,
)

data class HubCareTab(
    val residentId: String,
    val from: String? = null,
    val to: String? = null,
    val observedFrom: String? = null,
    val summaries: List<HubCareDay> = emptyList(),
    val avgMinutesPerDay: Double? = null,
    val proactiveShare: Double? = null,
)

data class IngestEnvelope(
    val sourceRecordId: String,
    val residentId: String,
    val observedOn: LocalDate,
    val data: Map<String, Any?>,
    val source: String = "insights",
    val modelVersion: String = "insights-0.1",
)
