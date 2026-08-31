package com.hub.care.application.service

import com.hub.care.application.dto.*
import com.hub.care.domain.model.CareSummary
import com.hub.care.domain.repository.CareSummaryRepository
import com.hub.shared.domain.ResidentId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class CareSummaryApplicationService(
    private val careSummaryRepository: CareSummaryRepository
) {

    @Transactional
    fun ingestCareSummary(request: IngestCareSummaryRequest): Pair<Boolean, CareSummaryResponse> {
        val residentId = ResidentId(request.residentId)
        val existing = careSummaryRepository.findByResidentAndDate(residentId, request.observedOn)
        val summary = if (existing != null) {
            existing.replay(
                sourceRecordId = request.sourceRecordId,
                totalMinutes = request.totalMinutes,
                proactiveMinutes = request.proactiveMinutes,
                roundsCount = request.roundsCount,
                notesCount = request.notesCount,
                source = request.source,
                modelVersion = request.modelVersion,
                confidence = request.confidence,
            )
        } else {
            CareSummary.create(
                sourceRecordId = request.sourceRecordId,
                residentId = residentId,
                observedOn = request.observedOn,
                totalMinutes = request.totalMinutes,
                proactiveMinutes = request.proactiveMinutes,
                roundsCount = request.roundsCount,
                notesCount = request.notesCount,
                source = request.source,
                modelVersion = request.modelVersion,
                confidence = request.confidence,
            )
        }
        return (existing == null) to toResponse(careSummaryRepository.save(summary))
    }

    @Transactional(readOnly = true)
    fun getCareSummaryRange(residentId: String, from: LocalDate, to: LocalDate): CareSummaryListResponse {
        val byDay = careSummaryRepository.findByResidentAndRange(ResidentId(residentId), from, to)
            .associateBy { it.observedOn }
        val summaries = datesInRange(from, to).map { date ->
            val summary = byDay[date]
            if (summary != null) {
                toResponse(summary)
            } else {
                CareSummaryResponse(
                    residentId = residentId,
                    observedOn = date,
                    totalMinutes = 0,
                    proactiveMinutes = 0,
                    roundsCount = 0,
                    notesCount = 0,
                    measured = false,
                )
            }
        }
        return CareSummaryListResponse(residentId, from, to, summaries)
    }

    private fun datesInRange(from: LocalDate, to: LocalDate): List<LocalDate> =
        generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) }
            .toList()

    private fun toResponse(s: CareSummary) = CareSummaryResponse(
        residentId = s.residentId.value,
        observedOn = s.observedOn,
        totalMinutes = s.totalMinutes,
        proactiveMinutes = s.proactiveMinutes,
        roundsCount = s.roundsCount,
        notesCount = s.notesCount
    )
}
