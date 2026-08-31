package com.hub.insights.rollup

import com.hub.insights.inbound.SleepSummaryData
import com.manahive.contracts.scene.StateKind
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class SleepRollupResult(
    val observedOn: LocalDate,
    val calmMinutes: Int,
    val restlessMinutes: Int,
    val awakeMinutes: Int,
    val outOfBedMinutes: Int,
    val bedExitCount: Int,
    val wakeCount: Int,
    val startedAt: LocalDateTime?,
    val endedAt: LocalDateTime?,
) {
    fun toData(): SleepSummaryData = SleepSummaryData(
        calmMinutes = calmMinutes,
        restlessMinutes = restlessMinutes,
        awakeMinutes = awakeMinutes,
        outOfBedMinutes = outOfBedMinutes,
        bedExitCount = bedExitCount,
        wakeCount = wakeCount,
        startedAt = startedAt,
        endedAt = endedAt,
    )
}

object SleepRollup {

    /**
     * [StateKind] hive. La latencia ([sleepOnsetMinutes]) se aplica **una vez**
     * al primer [StateKind.LYING] de la noche, no en cada vuelta a la cama.
     */
    fun compute(
        dwells: List<Dwell>,
        observedOn: LocalDate,
        zone: ZoneId,
        sleepOnsetMinutes: Int = 10,
    ): SleepRollupResult {
        var calmSec = 0L
        var restlessSec = 0L
        var awakeSec = 0L
        var outSec = 0L
        var exits = 0
        var wakes = 0
        var firstInBed: Instant? = null
        var lastInBed: Instant? = null
        var onsetApplied = false

        for (d in dwells) {
            when (d.kind) {
                StateKind.LYING -> {
                    if (!onsetApplied) {
                        val (pre, deep) = splitOnset(d.seconds, sleepOnsetMinutes)
                        restlessSec += pre
                        calmSec += deep
                        onsetApplied = true
                    } else {
                        calmSec += d.seconds
                    }
                    firstInBed = firstInBed ?: d.start
                    lastInBed = d.end
                }
                StateKind.ATTEMPTING_EXIT -> {
                    restlessSec += d.seconds
                    firstInBed = firstInBed ?: d.start
                    lastInBed = d.end
                    if (d.fromKind == StateKind.LYING) wakes += 1
                }
                StateKind.SITTING_IN_BED, StateKind.BED_EDGE -> {
                    awakeSec += d.seconds
                    firstInBed = firstInBed ?: d.start
                    lastInBed = d.end
                    if (d.fromKind == StateKind.LYING) wakes += 1
                }
                StateKind.STANDING, StateKind.ON_FLOOR, StateKind.IN_BATHROOM, StateKind.IN_ROOM,
                StateKind.IN_HALLWAY, StateKind.OUTDOOR, StateKind.ABSENT,
                StateKind.IN_CHAIR, StateKind.IN_WHEELCHAIR, StateKind.UNKNOWN -> {
                    outSec += d.seconds
                    if (d.fromKind?.inBed == true) exits += 1
                }
            }
        }

        return SleepRollupResult(
            observedOn = observedOn,
            calmMinutes = minutesFromSeconds(calmSec),
            restlessMinutes = minutesFromSeconds(restlessSec),
            awakeMinutes = minutesFromSeconds(awakeSec),
            outOfBedMinutes = minutesFromSeconds(outSec),
            bedExitCount = exits,
            wakeCount = wakes,
            startedAt = firstInBed?.atZone(zone)?.toLocalDateTime(),
            endedAt = lastInBed?.atZone(zone)?.toLocalDateTime(),
        )
    }

    private fun splitOnset(lyingSeconds: Long, thresholdMinutes: Int): Pair<Long, Long> {
        val threshold = thresholdMinutes * 60L
        if (lyingSeconds <= threshold) return lyingSeconds to 0L
        return threshold to (lyingSeconds - threshold)
    }
}
