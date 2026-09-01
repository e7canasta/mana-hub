package com.hub.views.readers

import com.hub.care.domain.repository.CareSummaryRepository
import com.hub.population.domain.repository.ResidentRepository
import com.hub.shared.domain.ResidentId
import com.hub.shared.time.DateRange
import com.hub.views.CareDayProjection
import com.hub.views.CareTabProjection
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class CareProjectionReader(
    private val careSummaryRepository: CareSummaryRepository,
    private val residentRepository: ResidentRepository,
) {

    private fun effectiveObservationFrom(residentId: String, from: LocalDate): LocalDate {
        val admission = residentRepository.findById(ResidentId(residentId))?.admissionDate
        return if (admission != null && admission.isAfter(from)) admission else from
    }

    private fun datesInRange(from: LocalDate, to: LocalDate): List<LocalDate> =
        DateRange.datesBetween(from, to)

    @Transactional(readOnly = true)
    fun getCareTab(residentId: String, from: LocalDate, to: LocalDate): CareTabProjection {
        val observedFrom = effectiveObservationFrom(residentId, from)
        val byDay = careSummaryRepository.findByResidentAndRange(
            ResidentId(residentId), observedFrom, to
        ).associateBy { it.observedOn }
        val days = datesInRange(observedFrom, to).map { date ->
            val summary = byDay[date]
            CareDayProjection(
                day = date.toString(),
                totalMinutes = summary?.totalMinutes ?: 0,
                proactiveMinutes = summary?.proactiveMinutes ?: 0,
                roundsCount = summary?.roundsCount ?: 0,
                notesCount = summary?.notesCount ?: 0,
                measured = summary != null,
            )
        }
        val measuredDays = days.filter { it.measured }
        val total = measuredDays.sumOf { it.totalMinutes }
        val proactive = measuredDays.sumOf { it.proactiveMinutes }
        val roundsObserved = measuredDays.any { it.roundsCount > 0 }
        return CareTabProjection(
            residentId = residentId,
            from = from.toString(),
            to = to.toString(),
            observedFrom = observedFrom.toString(),
            summaries = days,
            avgMinutesPerDay = if (measuredDays.isEmpty()) null else total.toDouble() / measuredDays.size,
            proactiveShare = when {
                !roundsObserved -> null
                total == 0 -> 0.0
                else -> proactive.toDouble() / total
            },
        )
    }
}
