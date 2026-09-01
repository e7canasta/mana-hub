package com.hub.insights.domain.rollup

import com.hub.insights.inbound.MobilitySummaryData
import java.time.LocalDate

data class MobilityRollupResult(
    val observedOn: LocalDate,
    val inBedMinutes: Int,
    val outOfBedMinutes: Int,
    val outOfSightMinutes: Int,
    val walkingMinutes: Int,
    val transferCount: Int,
) {
    /** Distancia no es un hecho: el SOR guarda 0. El estimado vive en insights. */
    fun toData(): MobilitySummaryData = MobilitySummaryData(
        inBedMinutes = inBedMinutes,
        outOfBedMinutes = outOfBedMinutes,
        outOfSightMinutes = outOfSightMinutes,
        walkingMinutes = walkingMinutes,
        distanceMeters = 0.0,
        transferCount = transferCount,
    )
}

object MobilityRollup {

    fun compute(dwells: List<Dwell>, observedOn: LocalDate): MobilityRollupResult {
        var inBed = 0L
        var outOfBed = 0L
        var outOfSight = 0L
        var walking = 0L
        var transfers = 0

        for (d in dwells) {
            when {
                d.kind.inBed -> inBed += d.seconds
                d.kind.outOfRoom -> {
                    walking += d.seconds
                    outOfBed += d.seconds
                    if (d.kind.outOfSight) outOfSight += d.seconds
                }
                d.kind.outOfSight -> outOfSight += d.seconds
                else -> outOfBed += d.seconds
            }
            val fromInBed = d.fromKind?.inBed
            if (fromInBed != null && fromInBed != d.kind.inBed) transfers += 1
        }

        return MobilityRollupResult(
            observedOn = observedOn,
            inBedMinutes = minutesFromSeconds(inBed),
            outOfBedMinutes = minutesFromSeconds(outOfBed),
            outOfSightMinutes = minutesFromSeconds(outOfSight),
            walkingMinutes = minutesFromSeconds(walking),
            transferCount = transfers,
        )
    }
}
