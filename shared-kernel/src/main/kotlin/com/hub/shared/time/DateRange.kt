package com.hub.shared.time

import java.time.LocalDate

object DateRange {
    fun datesBetween(from: LocalDate, to: LocalDate): List<LocalDate> =
        generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) }
            .toList()
}
