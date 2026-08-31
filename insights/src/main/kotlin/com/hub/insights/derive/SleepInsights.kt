package com.hub.insights.derive

import com.hub.insights.inbound.HubSleepDay
import java.time.LocalTime

data class SleepDerived(
    val avgCalmMinutes7d: Int?,
    val deltaCalmMinutesWoW: Int?,
    val avgRestlessMinutes7d: Int?,
    val avgAsleepMinutes7d: Int?,
    val restlessShare: Double?,
    val avgBedExits: Double?,
    val maxBedExits: Int?,
    val avgTimeInBedMinutes: Int?,
    val sleepEfficiency: Double?,
    val habitualFrom: LocalTime?,
    val habitualTo: LocalTime?,
)

object SleepInsights {

    fun derive(days: List<HubSleepDay>): SleepDerived {
        val ordered = days.filter { it.measured }.sortedBy { it.day }
        if (ordered.isEmpty()) {
            return SleepDerived(null, null, null, null, null, null, null, null, null, null, null)
        }
        val last7 = ordered.takeLast(7)
        val prev7 = ordered.dropLast(7).takeLast(7)

        val avgCalm = avgOrNull(last7.map { it.calmMinutes })
        val prevCalm = avgOrNull(prev7.map { it.calmMinutes })
        val delta = if (avgCalm != null && prevCalm != null) avgCalm - prevCalm else null

        val avgRestless = avgOrNull(last7.map { it.restlessMinutes })
        val avgAsleep = avgOrNull(last7.map { it.calmMinutes + it.restlessMinutes })
        val calmSum = last7.sumOf { it.calmMinutes }
        val restlessSum = last7.sumOf { it.restlessMinutes }
        val asleep = calmSum + restlessSum
        val share = if (asleep > 0) restlessSum.toDouble() / asleep else null

        val inBed = last7.map { it.calmMinutes + it.restlessMinutes + it.awakeMinutes }
        val avgInBed = avgOrNull(inBed)
        val asleepLast = last7.sumOf { it.calmMinutes + it.restlessMinutes }
        val inBedSum = inBed.sum()
        val efficiency = if (inBedSum > 0) asleepLast.toDouble() / inBedSum else null

        return SleepDerived(
            avgCalmMinutes7d = avgCalm,
            deltaCalmMinutesWoW = delta,
            avgRestlessMinutes7d = avgRestless,
            avgAsleepMinutes7d = avgAsleep,
            restlessShare = share,
            avgBedExits = last7.map { it.bedExitCount }.average().takeIf { last7.isNotEmpty() },
            maxBedExits = last7.maxOfOrNull { it.bedExitCount },
            avgTimeInBedMinutes = avgInBed,
            sleepEfficiency = efficiency,
            habitualFrom = medianTime(ordered.mapNotNull { it.startedAt?.toLocalTime() }),
            habitualTo = medianTime(ordered.mapNotNull { it.endedAt?.toLocalTime() }),
        )
    }

    private fun avgOrNull(values: List<Int>): Int? =
        if (values.isEmpty()) null else values.average().toInt()

    private fun medianTime(times: List<LocalTime>): LocalTime? {
        if (times.isEmpty()) return null
        val sorted = times.sorted()
        return sorted[sorted.size / 2]
    }
}

object CareInsights {
    fun avgMinutes(days: List<Int>): Double? =
        if (days.isEmpty()) null else days.average()

    fun proactiveShare(total: Int, proactive: Int): Double? =
        if (total <= 0) null else proactive.toDouble() / total
}

object MobilityInsights {
    fun avgWalking(minutes: List<Int>): Int? =
        if (minutes.isEmpty()) null else minutes.average().toInt()

    fun avgDistance(meters: List<Double>): Double? =
        if (meters.isEmpty()) null else meters.average()
}
