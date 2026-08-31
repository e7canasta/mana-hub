package com.hub.insights.rollup

import java.time.LocalDate

data class CareRollupResult(
    val observedOn: LocalDate,
    val totalMinutes: Int,
    val proactiveMinutes: Int,
    val roundsCount: Int,
    val notesCount: Int,
    val visitCount: Int,
)

object CareRollup {

    /**
     * Cuidado visto por escena: StaffPresenceDetected / StaffLeftDetected.
     * [proactiveMinutes] y [roundsCount] quedan 0 hasta cruzar con rondas:
     * no se publica un share de 0 % como si la residencia no hiciera rondas.
     */
    fun compute(visits: List<StaffVisit>, observedOn: LocalDate): CareRollupResult {
        val totalSec = visits.sumOf { it.seconds }
        return CareRollupResult(
            observedOn = observedOn,
            totalMinutes = minutesFromSeconds(totalSec),
            proactiveMinutes = 0,
            roundsCount = 0,
            notesCount = 0,
            visitCount = visits.size,
        )
    }
}
