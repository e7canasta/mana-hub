package com.hub.views.readers

import com.hub.observation.domain.repository.SummaryRepository
import com.hub.population.domain.repository.ResidentRepository
import com.hub.shared.domain.ResidentId
import com.hub.shared.time.DateRange
import com.hub.views.BathroomDayProjection
import com.hub.views.BathroomTabProjection
import com.hub.views.MobilityDayProjection
import com.hub.views.MobilityTabProjection
import com.hub.views.SleepDayProjection
import com.hub.views.SleepTabProjection
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class DailySummaryProjectionReader(
    private val summaryRepository: SummaryRepository,
    private val residentRepository: ResidentRepository,
) {

    private fun effectiveObservationFrom(residentId: String, from: LocalDate): LocalDate {
        val admission = residentRepository.findById(ResidentId(residentId))?.admissionDate
        return if (admission != null && admission.isAfter(from)) admission else from
    }

    private fun datesInRange(from: LocalDate, to: LocalDate): List<LocalDate> =
        DateRange.datesBetween(from, to)

    @Transactional(readOnly = true)
    fun getSleepTab(residentId: String, from: LocalDate, to: LocalDate): SleepTabProjection {
        val observedFrom = effectiveObservationFrom(residentId, from)
        val byDay = summaryRepository.findSleepByResidentAndRange(
            ResidentId(residentId), observedFrom, to
        ).associateBy { it.observedOn }
        val days = datesInRange(observedFrom, to).map { date ->
            val summary = byDay[date]
            SleepDayProjection(
                day = date.toString(),
                calmMinutes = summary?.calmMinutes ?: 0,
                restlessMinutes = summary?.restlessMinutes ?: 0,
                awakeMinutes = summary?.awakeMinutes ?: 0,
                outOfBedMinutes = summary?.outOfBedMinutes ?: 0,
                bedExitCount = summary?.bedExitCount ?: 0,
                wakeCount = summary?.wakeCount ?: 0,
                startedAt = summary?.startedAt,
                endedAt = summary?.endedAt,
                measured = summary != null,
            )
        }
        return SleepTabProjection(
            residentId = residentId,
            from = from.toString(),
            to = to.toString(),
            observedFrom = observedFrom.toString(),
            summaries = days,
        )
    }

    @Transactional(readOnly = true)
    fun getMobilityTab(residentId: String, from: LocalDate, to: LocalDate): MobilityTabProjection {
        val observedFrom = effectiveObservationFrom(residentId, from)
        val byDay = summaryRepository.findMobilityByResidentAndRange(
            ResidentId(residentId), observedFrom, to
        ).associateBy { it.observedOn }
        val days = datesInRange(observedFrom, to).map { date ->
            val summary = byDay[date]
            MobilityDayProjection(
                day = date.toString(),
                walkingMinutes = summary?.walkingMinutes ?: 0,
                distanceMeters = summary?.distanceMeters ?: 0.0,
                transferCount = summary?.transferCount ?: 0,
                outOfBedMinutes = summary?.outOfBedMinutes ?: 0,
                inBedMinutes = summary?.inBedMinutes ?: 0,
                outOfSightMinutes = summary?.outOfSightMinutes ?: 0,
                measured = summary != null,
            )
        }
        return MobilityTabProjection(
            residentId = residentId,
            from = from.toString(),
            to = to.toString(),
            observedFrom = observedFrom.toString(),
            summaries = days,
        )
    }

    @Transactional(readOnly = true)
    fun getBathroomTab(residentId: String, from: LocalDate, to: LocalDate): BathroomTabProjection {
        val observedFrom = effectiveObservationFrom(residentId, from)
        val byDay = summaryRepository.findBathroomByResidentAndRange(
            ResidentId(residentId), observedFrom, to
        ).associateBy { it.observedOn }
        val days = datesInRange(observedFrom, to).map { date ->
            val summary = byDay[date]
            BathroomDayProjection(
                day = date.toString(),
                visitCount = summary?.visitCount ?: 0,
                nightVisitCount = summary?.nightVisitCount ?: 0,
                assistedCount = summary?.assistedCount ?: 0,
                totalMinutes = summary?.totalMinutes ?: 0,
                measured = summary != null,
            )
        }
        return BathroomTabProjection(
            residentId = residentId,
            from = from.toString(),
            to = to.toString(),
            observedFrom = observedFrom.toString(),
            summaries = days,
        )
    }
}
