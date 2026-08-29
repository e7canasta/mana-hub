package com.hub.observation.application.service

import com.hub.observation.application.dto.*
import com.hub.observation.domain.model.*
import com.hub.observation.domain.repository.SummaryRepository
import com.hub.shared.domain.ResidentId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SummaryQueryService(
    private val summaryRepository: SummaryRepository
) {

    @Transactional(readOnly = true)
    fun getSleepSummary(residentId: String, date: java.time.LocalDate): SleepSummaryResponse? {
        return summaryRepository.findSleepByResidentAndDate(ResidentId(residentId), date)?.toResponse()
    }

    @Transactional(readOnly = true)
    fun getSleepSummaryRange(residentId: String, from: java.time.LocalDate, to: java.time.LocalDate): SleepSummaryListResponse {
        val summaries = summaryRepository.findSleepByResidentAndRange(ResidentId(residentId), from, to)
            .map { it.toResponse() }
        return SleepSummaryListResponse(residentId, from, to, summaries)
    }

    @Transactional(readOnly = true)
    fun getMobilitySummary(residentId: String, date: java.time.LocalDate): MobilitySummaryResponse? {
        return summaryRepository.findMobilityByResidentAndDate(ResidentId(residentId), date)?.toResponse()
    }

    @Transactional(readOnly = true)
    fun getMobilitySummaryRange(residentId: String, from: java.time.LocalDate, to: java.time.LocalDate): MobilitySummaryListResponse {
        val summaries = summaryRepository.findMobilityByResidentAndRange(ResidentId(residentId), from, to)
            .map { it.toResponse() }
        return MobilitySummaryListResponse(residentId, from, to, summaries)
    }

    @Transactional(readOnly = true)
    fun getBathroomSummary(residentId: String, date: java.time.LocalDate): BathroomSummaryResponse? {
        return summaryRepository.findBathroomByResidentAndDate(ResidentId(residentId), date)?.toResponse()
    }

    @Transactional(readOnly = true)
    fun getBathroomSummaryRange(residentId: String, from: java.time.LocalDate, to: java.time.LocalDate): BathroomSummaryListResponse {
        val summaries = summaryRepository.findBathroomByResidentAndRange(ResidentId(residentId), from, to)
            .map { it.toResponse() }
        return BathroomSummaryListResponse(residentId, from, to, summaries)
    }

    private fun SleepSummary.toResponse() = SleepSummaryResponse(
        residentId = residentId.value, observedOn = observedOn, calmMinutes = calmMinutes,
        restlessMinutes = restlessMinutes, awakeMinutes = awakeMinutes, outOfBedMinutes = outOfBedMinutes,
        bedExitCount = bedExitCount, wakeCount = wakeCount, startedAt = startedAt, endedAt = endedAt
    )

    private fun MobilitySummary.toResponse() = MobilitySummaryResponse(
        residentId = residentId.value, observedOn = observedOn, walkingMinutes = walkingMinutes,
        distanceMeters = distanceMeters, transferCount = transferCount, outOfBedMinutes = outOfBedMinutes
    )

    private fun BathroomSummary.toResponse() = BathroomSummaryResponse(
        residentId = residentId.value, observedOn = observedOn, visitCount = visitCount,
        nightVisitCount = nightVisitCount
    )
}
