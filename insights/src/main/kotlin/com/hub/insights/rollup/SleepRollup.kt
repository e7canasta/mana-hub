package com.hub.insights.rollup

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
    fun toPayload(): Map<String, Any?> = mapOf(
        "calmMinutes" to calmMinutes,
        "restlessMinutes" to restlessMinutes,
        "awakeMinutes" to awakeMinutes,
        "outOfBedMinutes" to outOfBedMinutes,
        "bedExitCount" to bedExitCount,
        "wakeCount" to wakeCount,
        "startedAt" to startedAt?.toString(),
        "endedAt" to endedAt?.toString(),
    )
}

object SleepRollup {

    /**
     * Solo [StateKind] de hive:
     * - Lying ≥ [deepSleepAfterMinutes] → profundo (calm)
     * - Lying corto / AttemptingExit → inquieto
     * - SittingInBed / BedEdge → despierto en cama
     */
    fun compute(
        dwells: List<Dwell>,
        observedOn: LocalDate,
        zone: ZoneId,
        deepSleepAfterMinutes: Int = 10,
    ): SleepRollupResult {
        var calm = 0
        var restless = 0
        var awake = 0
        var out = 0
        var exits = 0
        var wakes = 0
        var firstInBed: Instant? = null
        var lastInBed: Instant? = null

        for (d in dwells) {
            when {
                d.kind == StateKind.LYING -> {
                    val (pre, deep) = splitDeepSleep(d.minutes, deepSleepAfterMinutes)
                    restless += pre
                    calm += deep
                    firstInBed = firstInBed ?: d.start
                    lastInBed = d.end
                }
                d.kind.restlessInBed -> {
                    restless += d.minutes
                    firstInBed = firstInBed ?: d.start
                    lastInBed = d.end
                    if (d.fromKind == StateKind.LYING) wakes += 1
                }
                d.kind.awakeInBed -> {
                    awake += d.minutes
                    firstInBed = firstInBed ?: d.start
                    lastInBed = d.end
                    if (d.fromKind == StateKind.LYING) wakes += 1
                }
                else -> {
                    out += d.minutes
                    if (d.fromKind?.inBed == true && !d.kind.inBed) exits += 1
                }
            }
        }

        return SleepRollupResult(
            observedOn = observedOn,
            calmMinutes = calm,
            restlessMinutes = restless,
            awakeMinutes = awake,
            outOfBedMinutes = out,
            bedExitCount = exits,
            wakeCount = wakes,
            startedAt = firstInBed?.atZone(zone)?.toLocalDateTime(),
            endedAt = lastInBed?.atZone(zone)?.toLocalDateTime(),
        )
    }

    private fun splitDeepSleep(lyingMinutes: Int, threshold: Int): Pair<Int, Int> {
        if (lyingMinutes <= threshold) return lyingMinutes to 0
        return threshold to (lyingMinutes - threshold)
    }
}
