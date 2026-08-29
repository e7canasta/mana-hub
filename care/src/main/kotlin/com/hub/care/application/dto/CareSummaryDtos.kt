package com.hub.care.application.dto

import java.time.LocalDate

data class CareSummaryResponse(
    val residentId: String,
    val observedOn: LocalDate,
    val totalMinutes: Int,
    val proactiveMinutes: Int,
    val roundsCount: Int,
    val notesCount: Int
)

data class CareSummaryListResponse(
    val residentId: String,
    val from: LocalDate,
    val to: LocalDate,
    val summaries: List<CareSummaryResponse>
)

data class IngestCareSummaryRequest(
    val sourceRecordId: String,
    val residentId: String,
    val observedOn: LocalDate,
    val totalMinutes: Int,
    val proactiveMinutes: Int = 0,
    val roundsCount: Int = 0,
    val notesCount: Int = 0,
    val source: String? = null,
    val modelVersion: String? = null,
    val confidence: Double? = null
)
