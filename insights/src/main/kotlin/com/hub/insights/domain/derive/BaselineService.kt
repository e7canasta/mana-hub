package com.hub.insights.domain.derive

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Baseline(
    val admissionDate: LocalDate?,
    val observedFrom: LocalDate,
    val observedDays: Int,
    val ready: Boolean,
)

object BaselineService {

    fun of(
        admissionDate: LocalDate?,
        from: LocalDate,
        to: LocalDate,
        minDays: Int = 7,
        today: LocalDate = LocalDate.now(),
    ): Baseline {
        val observedFrom = if (admissionDate != null && admissionDate.isAfter(from)) admissionDate else from
        val observedDays = if (admissionDate == null) {
            ChronoUnit.DAYS.between(from, to).toInt() + 1
        } else {
            ChronoUnit.DAYS.between(admissionDate, today).toInt().coerceAtLeast(0)
        }
        val ready = admissionDate != null && observedDays >= minDays
        return Baseline(
            admissionDate = admissionDate,
            observedFrom = observedFrom,
            observedDays = observedDays,
            ready = ready,
        )
    }
}
