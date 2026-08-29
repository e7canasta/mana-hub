package com.hub.care.domain.model

import com.hub.shared.domain.ResidentId
import java.time.Instant
import java.time.LocalDate

class CareSummary private constructor(
    override val id: CareSummaryId,
    val sourceRecordId: String,
    val residentId: ResidentId,
    val observedOn: LocalDate,
    val totalMinutes: Int,
    val proactiveMinutes: Int,
    val roundsCount: Int,
    val notesCount: Int,
    val source: String?,
    val modelVersion: String?,
    val confidence: Double?,
    override var version: Long
) : com.hub.shared.domain.AggregateRoot<CareSummaryId>() {

    companion object {
        fun create(
            sourceRecordId: String,
            residentId: ResidentId,
            observedOn: LocalDate,
            totalMinutes: Int,
            proactiveMinutes: Int,
            roundsCount: Int,
            notesCount: Int,
            source: String? = null,
            modelVersion: String? = null,
            confidence: Double? = null
        ): CareSummary = CareSummary(
            id = CareSummaryId.random(),
            sourceRecordId = sourceRecordId,
            residentId = residentId,
            observedOn = observedOn,
            totalMinutes = totalMinutes,
            proactiveMinutes = proactiveMinutes,
            roundsCount = roundsCount,
            notesCount = notesCount,
            source = source,
            modelVersion = modelVersion,
            confidence = confidence,
            version = 0
        )

        fun reconstitute(
            id: CareSummaryId,
            sourceRecordId: String,
            residentId: ResidentId,
            observedOn: LocalDate,
            totalMinutes: Int,
            proactiveMinutes: Int,
            roundsCount: Int,
            notesCount: Int,
            source: String?,
            modelVersion: String?,
            confidence: Double?,
            version: Long
        ): CareSummary = CareSummary(
            id, sourceRecordId, residentId, observedOn, totalMinutes, proactiveMinutes,
            roundsCount, notesCount, source, modelVersion, confidence, version
        )
    }
}
