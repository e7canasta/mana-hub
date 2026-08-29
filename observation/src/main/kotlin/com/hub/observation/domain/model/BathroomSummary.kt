package com.hub.observation.domain.model

import com.hub.shared.domain.Identifier
import com.hub.shared.domain.ResidentId
import java.time.LocalDate

data class BathroomSummary(
    val id: Identifier,
    val sourceRecordId: String,
    val residentId: ResidentId,
    val observedOn: LocalDate,
    val visitCount: Int,
    val nightVisitCount: Int,
    val assistedCount: Int,
    val totalMinutes: Int,
    val source: String?,
    val modelVersion: String?,
    val confidence: Double?
) {
    companion object {
        fun create(
            sourceRecordId: String, residentId: ResidentId, observedOn: LocalDate,
            visitCount: Int, nightVisitCount: Int, assistedCount: Int, totalMinutes: Int,
            source: String?, modelVersion: String?, confidence: Double?
        ) = BathroomSummary(
            id = Identifier.random(), sourceRecordId = sourceRecordId, residentId = residentId,
            observedOn = observedOn, visitCount = visitCount, nightVisitCount = nightVisitCount,
            assistedCount = assistedCount, totalMinutes = totalMinutes,
            source = source, modelVersion = modelVersion, confidence = confidence
        )
    }
}
