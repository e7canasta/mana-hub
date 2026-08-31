package com.hub.insights.rollup

import com.hub.insights.inbound.BathroomSummaryData
import com.manahive.contracts.scene.StateKind
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class BathroomRollupResult(
    val observedOn: LocalDate,
    val visitCount: Int,
    val nightVisitCount: Int,
    val assistedCount: Int,
    val totalMinutes: Int,
) {
    fun toData(): BathroomSummaryData = BathroomSummaryData(
        visitCount = visitCount,
        nightVisitCount = nightVisitCount,
        assistedCount = assistedCount,
        totalMinutes = totalMinutes,
    )
}

object BathroomRollup {

    private val nightStart = LocalTime.of(22, 0)
    private val nightEnd = LocalTime.of(6, 0)

    fun compute(dwells: List<Dwell>, observedOn: LocalDate, zone: ZoneId): BathroomRollupResult {
        val visits = dwells.filter {
            it.kind == StateKind.IN_BATHROOM && it.fromKind != StateKind.IN_BATHROOM
        }
        val seconds = dwells.filter { it.kind == StateKind.IN_BATHROOM }.sumOf { it.seconds }
        val night = visits.count { isNight(it.start, zone) }
        return BathroomRollupResult(
            observedOn = observedOn,
            visitCount = visits.size,
            nightVisitCount = night,
            assistedCount = 0,
            totalMinutes = minutesFromSeconds(seconds),
        )
    }

    private fun isNight(at: java.time.Instant, zone: ZoneId): Boolean {
        val t = at.atZone(zone).toLocalTime()
        return !t.isBefore(nightStart) || t.isBefore(nightEnd)
    }
}
