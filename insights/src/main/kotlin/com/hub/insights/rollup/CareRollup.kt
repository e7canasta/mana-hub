package com.hub.insights.rollup

import java.time.LocalDate

data class CareRollupResult(
    val observedOn: LocalDate,
    val totalMinutes: Int,
    val proactiveMinutes: Int,
    val roundsCount: Int,
    val notesCount: Int,
    val visitCount: Int,
) {
    fun toPayload(): Map<String, Any?> = mapOf(
        "totalMinutes" to totalMinutes,
        "proactiveMinutes" to proactiveMinutes,
        "roundsCount" to roundsCount,
        "notesCount" to notesCount,
    )
}

object CareRollup {

    /**
     * Cuidado visto por escena: [SceneEvent.StaffPresenceDetected] / StaffLeftDetected.
     * Todo es reactivo/espontáneo (no ronda) hasta cruzar con `rounds`.
     */
    fun compute(visits: List<StaffVisit>, observedOn: LocalDate): CareRollupResult {
        val total = visits.sumOf { it.minutes }
        return CareRollupResult(
            observedOn = observedOn,
            totalMinutes = total,
            proactiveMinutes = 0,
            roundsCount = 0,
            notesCount = 0,
            visitCount = visits.size,
        )
    }
}
