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
    fun ingestCareSummary(request: IngestCareSummaryRequest): CareSummaryResponse {
        val summary = CareSummary.create(
            sourceRecordId = request.sourceRecordId,
            residentId = ResidentId(request.residentId),
            observedOn = request.observedOn,
            totalMinutes = request.totalMinutes,
            proactiveMinutes = request.proactiveMinutes,
            roundsCount = request.roundsCount,
            notesCount = request.notesCount,
            source = request.source,
            modelVersion = request.modelVersion,
            confidence = request.confidence
        )
        return toResponse(careSummaryRepository.save(summary))
    }

    @Transactional(readOnly = true)
    fun getCareSummaryRange(residentId: String, from: LocalDate, to: LocalDate): CareSummaryListResponse {
        val summaries = careSummaryRepository.findByResidentAndRange(ResidentId(residentId), from, to)
            .map { toResponse(it) }
        return CareSummaryListResponse(residentId, from, to, summaries)
    }

    private fun toResponse(s: CareSummary) = CareSummaryResponse(
        residentId = s.residentId.value,
        observedOn = s.observedOn,
        totalMinutes = s.totalMinutes,
        proactiveMinutes = s.proactiveMinutes,
        roundsCount = s.roundsCount,
        notesCount = s.notesCount
    )
}
