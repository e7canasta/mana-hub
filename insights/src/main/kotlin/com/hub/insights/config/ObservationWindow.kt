package com.hub.insights.config

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Ventana de observación de una noche y de un día calendario en TZ de la residencia.
 *
 * Sueño de [observedOn]: [observedOn 19:00, observedOn+1 12:00).
 * Movilidad / baño / cuidado de [observedOn]: [observedOn 00:00, observedOn+1 00:00).
 */
class ObservationWindow(
    private val zone: ZoneId,
    private val sleepStart: java.time.LocalTime,
    private val sleepEnd: java.time.LocalTime,
) {
    fun sleepBounds(observedOn: LocalDate): ClosedRange<Instant> {
        val start = LocalDateTime.of(observedOn, sleepStart).atZone(zone).toInstant()
        val end = LocalDateTime.of(observedOn.plusDays(1), sleepEnd).atZone(zone).toInstant()
        return start..end
    }

    fun calendarDayBounds(observedOn: LocalDate): ClosedRange<Instant> {
        val start = observedOn.atStartOfDay(zone).toInstant()
        val end = observedOn.plusDays(1).atStartOfDay(zone).toInstant()
        return start..end
    }

    fun localDate(instant: Instant): LocalDate =
        instant.atZone(zone).toLocalDate()

    companion object {
        fun from(properties: InsightsProperties) = ObservationWindow(
            zone = properties.zoneId,
            sleepStart = properties.sleepWindow.startTime,
            sleepEnd = properties.sleepWindow.endTime,
        )
    }
}
