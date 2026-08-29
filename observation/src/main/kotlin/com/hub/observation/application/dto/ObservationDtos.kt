package com.hub.observation.application.dto

import java.time.Instant
import java.time.LocalDate

data class IngestEventRequest(
    val sourceEventId: String,
    val monitorKey: String,
    val bedId: String? = null,
    val residentId: String? = null,
    val kind: String,
    val roomState: String? = null,
    val state: String? = null,
    val sleeping: Boolean? = null,
    val occurredAt: Instant,
    val payloadJson: String = "{}"
)

data class IngestSummaryRequest<T>(
    val sourceRecordId: String,
    val residentId: String,
    val observedOn: LocalDate,
    val data: T,
    val source: String? = null,
    val modelVersion: String? = null,
    val confidence: Double? = null
)

data class SleepSummaryData(
    val calmMinutes: Int = 0,
    val restlessMinutes: Int = 0,
    val awakeMinutes: Int = 0,
    val outOfBedMinutes: Int = 0,
    val bedExitCount: Int = 0,
    val wakeCount: Int = 0
)

data class MobilitySummaryData(
    val inBedMinutes: Int = 0,
    val outOfBedMinutes: Int = 0,
    val outOfSightMinutes: Int = 0,
    val walkingMinutes: Int = 0,
    val distanceMeters: Double = 0.0,
    val transferCount: Int = 0
)

data class BathroomSummaryData(
    val visitCount: Int = 0,
    val nightVisitCount: Int = 0,
    val assistedCount: Int = 0,
    val totalMinutes: Int = 0
)

data class BedStateResponse(
    val bedId: String,
    val residentId: String?,
    val roomState: String?,
    val state: String?,
    val sleeping: Boolean?,
    val stateSince: Instant
)

data class SleepSummaryResponse(
    val residentId: String,
    val observedOn: LocalDate,
    val calmMinutes: Int,
    val restlessMinutes: Int,
    val awakeMinutes: Int,
    val outOfBedMinutes: Int,
    val bedExitCount: Int,
    val wakeCount: Int
)

data class SleepSummaryListResponse(
    val residentId: String,
    val from: LocalDate,
    val to: LocalDate,
    val summaries: List<SleepSummaryResponse>
)

data class MobilitySummaryResponse(
    val residentId: String,
    val observedOn: LocalDate,
    val walkingMinutes: Int,
    val distanceMeters: Double,
    val transferCount: Int,
    val outOfBedMinutes: Int
)

data class MobilitySummaryListResponse(
    val residentId: String,
    val from: LocalDate,
    val to: LocalDate,
    val summaries: List<MobilitySummaryResponse>
)

data class BathroomSummaryResponse(
    val residentId: String,
    val observedOn: LocalDate,
    val visitCount: Int,
    val nightVisitCount: Int
)

data class BathroomSummaryListResponse(
    val residentId: String,
    val from: LocalDate,
    val to: LocalDate,
    val summaries: List<BathroomSummaryResponse>
)

data class IngestNotificationRequest(
    val category: String,
    val bedId: String? = null,
    val residentId: String? = null,
    val eventType: String,
    val timestamp: Instant,
    val ruleId: String? = null,
    val riskLevel: String? = null,
    val payloadJson: String = "{}"
)

data class NotificationResponse(
    val id: String,
    val category: String,
    val bedId: String?,
    val residentId: String?,
    val eventType: String,
    val timestamp: Instant,
    val ruleId: String?,
    val riskLevel: String?
)
