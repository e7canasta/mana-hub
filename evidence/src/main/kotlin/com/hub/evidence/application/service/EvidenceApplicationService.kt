package com.hub.evidence.application.service

import com.hub.evidence.application.dto.*
import com.hub.evidence.domain.model.*
import com.hub.evidence.domain.repository.ClipWindowRepository
import com.hub.evidence.domain.repository.EvidenceRepository
import com.hub.evidence.domain.repository.TimelineRepository
import com.hub.population.domain.model.ResidentId
import com.hub.residence.domain.model.BedId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EvidenceApplicationService(
    private val evidenceRepository: EvidenceRepository,
    private val timelineRepository: TimelineRepository,
    private val clipWindowRepository: ClipWindowRepository
) {

    @Transactional
    fun createEvidence(request: CreateEvidenceRequest): EvidenceResponse {
        val evidence = Evidence.create(
            bedId = BedId(request.bedId), residentId = ResidentId(request.residentId),
            evidenceType = request.evidenceType, category = request.category, timestamp = request.timestamp
        )
        return evidenceRepository.save(evidence).toResponse()
    }

    @Transactional
    fun createTimeline(bedId: String, residentId: String): TimelineResponse {
        val timeline = Timeline.create(BedId(bedId), ResidentId(residentId), java.time.Instant.now())
        return timelineRepository.save(timeline).toResponse()
    }

    @Transactional
    fun closeTimeline(timelineId: String): TimelineResponse {
        val timeline = timelineRepository.findById(EvidenceId(timelineId))
            ?: throw IllegalArgumentException("Timeline not found: $timelineId")
        return timelineRepository.save(timeline.close()).toResponse()
    }

    @Transactional
    fun createClipWindow(bedId: String, residentId: String): ClipWindowResponse {
        val clipWindow = ClipWindow.create(BedId(bedId), ResidentId(residentId))
        return clipWindowRepository.save(clipWindow).toResponse()
    }

    @Transactional
    fun closeClipWindow(windowId: String): ClipWindowResponse {
        val clipWindow = clipWindowRepository.findById(EvidenceId(windowId))
            ?: throw IllegalArgumentException("ClipWindow not found: $windowId")
        return clipWindowRepository.save(clipWindow.close()).toResponse()
    }

    @Transactional(readOnly = true)
    fun getOpenClipWindows(bedId: String): List<ClipWindowResponse> {
        val clipWindow = clipWindowRepository.findOpenByBedId(BedId(bedId))
        return clipWindow?.let { listOf(it.toResponse()) } ?: emptyList()
    }

    private fun Evidence.toResponse() = EvidenceResponse(
        id = id.value, bedId = bedId.value, residentId = residentId.value, evidenceType = evidenceType,
        category = category, riskLevel = riskLevel, timestamp = timestamp
    )

    private fun Timeline.toResponse() = TimelineResponse(
        id = id.value, bedId = bedId.value, residentId = residentId.value,
        windowStart = windowStart, windowEnd = windowEnd, isOpen = isOpen
    )

    private fun ClipWindow.toResponse() = ClipWindowResponse(
        id = id.value, bedId = bedId.value, residentId = residentId.value,
        startedAt = startedAt, endedAt = endedAt, state = state
    )
}
