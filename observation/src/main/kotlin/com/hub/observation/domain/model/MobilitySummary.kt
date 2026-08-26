package com.hub.observation.domain.model

import com.hub.shared.domain.Identifier
import com.hub.population.domain.model.ResidentId
import java.time.LocalDate

data class MobilitySummary(
    val id: Identifier,
    val sourceRecordId: String,
    val residentId: ResidentId,
    val observedOn: LocalDate,
    val inBedMinutes: Int,
    val outOfBedMinutes: Int,
    val outOfSightMinutes: Int,
    val walkingMinutes: Int,
    val distanceMeters: Double,
    val transferCount: Int,
    val source: String?,
    val modelVersion: String?,
    val confidence: Double?
) {
    companion object {
        fun create(
            sourceRecordId: String, residentId: ResidentId, observedOn: LocalDate,
            inBedMinutes: Int, outOfBedMinutes: Int, outOfSightMinutes: Int,
            walkingMinutes: Int, distanceMeters: Double, transferCount: Int,
            source: String?, modelVersion: String?, confidence: Double?
        ) = MobilitySummary(
            id = Identifier.random(), sourceRecordId = sourceRecordId, residentId = residentId,
            observedOn = observedOn, inBedMinutes = inBedMinutes, outOfBedMinutes = outOfBedMinutes,
            outOfSightMinutes = outOfSightMinutes, walkingMinutes = walkingMinutes,
            distanceMeters = distanceMeters, transferCount = transferCount,
            source = source, modelVersion = modelVersion, confidence = confidence
        )
    }
}
