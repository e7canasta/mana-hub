package com.hub.insights.rollup

import java.time.LocalDate

data class MobilityRollupResult(
    val observedOn: LocalDate,
    val inBedMinutes: Int,
    val outOfBedMinutes: Int,
    val outOfSightMinutes: Int,
    val walkingMinutes: Int,
    val distanceMeters: Double,
    val transferCount: Int,
) {
    fun toPayload(): Map<String, Any?> = mapOf(
        "inBedMinutes" to inBedMinutes,
        "outOfBedMinutes" to outOfBedMinutes,
        "outOfSightMinutes" to outOfSightMinutes,
        "walkingMinutes" to walkingMinutes,
        "distanceMeters" to distanceMeters,
        "transferCount" to transferCount,
    )
}

object MobilityRollup {

    /**
     * Andar = [StateKind.outOfRoom] (InHallway, Outdoor, Absent).
     * Standing / InRoom / InChair siguen en la habitación.
     */
    fun compute(
        dwells: List<Dwell>,
        observedOn: LocalDate,
        walkingMetersPerMinute: Double,
    ): MobilityRollupResult {
        var inBed = 0
        var outOfBed = 0
        var outOfSight = 0
        var walking = 0
        var transfers = 0

        for (d in dwells) {
            when {
                d.kind.inBed -> inBed += d.minutes
                d.kind.outOfRoom -> {
                    walking += d.minutes
                    outOfBed += d.minutes
                    if (d.kind.outOfSight) outOfSight += d.minutes
                }
                d.kind.outOfSight -> outOfSight += d.minutes
                else -> outOfBed += d.minutes
            }
            val fromInBed = d.fromKind?.inBed
            if (fromInBed != null && fromInBed != d.kind.inBed) transfers += 1
        }

        return MobilityRollupResult(
            observedOn = observedOn,
            inBedMinutes = inBed,
            outOfBedMinutes = outOfBed,
            outOfSightMinutes = outOfSight,
            walkingMinutes = walking,
            distanceMeters = walking * walkingMetersPerMinute,
            transferCount = transfers,
        )
    }
}
