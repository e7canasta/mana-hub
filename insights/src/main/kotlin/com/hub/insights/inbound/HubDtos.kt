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
    val isDischarged: Boolean? = null,
)

data class HubChart(
    val id: String,
    val fullName: String? = null,
    val admissionDate: LocalDate? = null,
)

/**
 * Anti-corruption: un campo canónico hive (`type`/`at`/`from`/`to`) y alias del GET aplanado.
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
    val measured: Boolean = true,
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
    val inBedMinutes: Int = 0,
    val outOfSightMinutes: Int = 0,
    val measured: Boolean = true,
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
    val assistedCount: Int = 0,
    val totalMinutes: Int = 0,
    val measured: Boolean = true,
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
    val measured: Boolean = true,
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

data class SleepSummaryData(
    val calmMinutes: Int = 0,
    val restlessMinutes: Int = 0,
    val awakeMinutes: Int = 0,
    val outOfBedMinutes: Int = 0,
    val bedExitCount: Int = 0,
    val wakeCount: Int = 0,
    val startedAt: LocalDateTime? = null,
    val endedAt: LocalDateTime? = null,
)

data class MobilitySummaryData(
    val inBedMinutes: Int = 0,
    val outOfBedMinutes: Int = 0,
    val outOfSightMinutes: Int = 0,
    val walkingMinutes: Int = 0,
    val distanceMeters: Double = 0.0,
    val transferCount: Int = 0,
)

data class BathroomSummaryData(
    val visitCount: Int = 0,
    val nightVisitCount: Int = 0,
    val assistedCount: Int = 0,
    val totalMinutes: Int = 0,
)

data class CareSummaryData(
    val totalMinutes: Int = 0,
    val proactiveMinutes: Int = 0,
    val roundsCount: Int = 0,
    val notesCount: Int = 0,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HubAlarmPresets(
    val residentId: String? = null,
    val riskLevel: String? = null,
    val overrides: Map<String, HubOverrideEntry> = emptyMap(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HubOverrideEntry(
    val warningAfterMinutes: Int? = null,
    val alertAfterMinutes: Int? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HubEpisodesTab(
    val residentId: String? = null,
    val episodes: List<HubEpisode> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HubEpisode(
    val id: String,
    val kind: String? = null,
    val severity: String? = null,
    val occurredAt: Instant,
    val selfRecovery: Boolean? = null,
    val injuryStatus: String? = null,
    val verdict: String? = null,
)

data class IngestEnvelope<T>(
    val sourceRecordId: String,
    val residentId: String,
    val observedOn: LocalDate,
    val data: T,
    val source: String = "insights",
    val modelVersion: String = "insights-0.1",
)
