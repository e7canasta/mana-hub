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
        val acc = SleepAccumulator()
        for (d in dwells) acc.accumulate(d, sleepOnsetMinutes)
        return acc.result(observedOn, zone)
    }

    private class SleepAccumulator {
        private var calmSec = 0L
        private var restlessSec = 0L
        private var awakeSec = 0L
        private var outSec = 0L
        private var exits = 0
        private var wakes = 0
        private var firstInBed: Instant? = null
        private var lastInBed: Instant? = null
        private var onsetApplied = false

        fun accumulate(d: Dwell, sleepOnsetMinutes: Int) {
            when (d.kind) {
                StateKind.LYING -> handleLying(d, sleepOnsetMinutes)
                StateKind.ATTEMPTING_EXIT -> handleRestless(d)
                StateKind.SITTING_IN_BED, StateKind.BED_EDGE -> handleAwake(d)
                else -> handleOut(d)
            }
        }

        private fun handleLying(d: Dwell, sleepOnsetMinutes: Int) {
            if (!onsetApplied) {
                val (pre, deep) = splitOnset(d.seconds, sleepOnsetMinutes)
                restlessSec += pre; calmSec += deep; onsetApplied = true
            } else {
                calmSec += d.seconds
            }
            trackInBed(d)
        }

        private fun handleRestless(d: Dwell) {
            restlessSec += d.seconds
            if (d.fromKind == StateKind.LYING) wakes += 1
            trackInBed(d)
        }

        private fun handleAwake(d: Dwell) {
            awakeSec += d.seconds
            if (d.fromKind == StateKind.LYING) wakes += 1
            trackInBed(d)
        }

        private fun handleOut(d: Dwell) {
            outSec += d.seconds
            if (d.fromKind?.inBed == true) exits += 1
        }

        private fun trackInBed(d: Dwell) {
            firstInBed = firstInBed ?: d.start
            lastInBed = d.end
        }

        fun result(observedOn: LocalDate, zone: ZoneId) = SleepRollupResult(
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
