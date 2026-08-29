package com.hub.observation.domain.model

import com.hub.shared.domain.Identifier
import com.hub.shared.domain.ResidentId
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

data class SleepSummary(
    val id: Identifier,
    val sourceRecordId: String,
    val residentId: ResidentId,
    val observedOn: LocalDate,
    val calmMinutes: Int,
    val restlessMinutes: Int,
    val awakeMinutes: Int,
    val outOfBedMinutes: Int,
    val bedExitCount: Int,
    val wakeCount: Int,
    val source: String?,
    val modelVersion: String?,
    val confidence: Double?,
    val startedAt: LocalDateTime? = null,
    val endedAt: LocalDateTime? = null
) {
    companion object {
        fun create(
            sourceRecordId: String, residentId: ResidentId, observedOn: LocalDate,
            calmMinutes: Int, restlessMinutes: Int, awakeMinutes: Int, outOfBedMinutes: Int,
            bedExitCount: Int, wakeCount: Int,
            source: String?, modelVersion: String?, confidence: Double?,
            startedAt: LocalDateTime? = null, endedAt: LocalDateTime? = null
        ) = SleepSummary(
            id = Identifier.random(), sourceRecordId = sourceRecordId, residentId = residentId,
            observedOn = observedOn, calmMinutes = calmMinutes, restlessMinutes = restlessMinutes,
            awakeMinutes = awakeMinutes, outOfBedMinutes = outOfBedMinutes,
            bedExitCount = bedExitCount, wakeCount = wakeCount,
            source = source, modelVersion = modelVersion, confidence = confidence,
            startedAt = startedAt, endedAt = endedAt
        )
    }
}
