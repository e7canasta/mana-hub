package com.hub.history.application.service

import com.hub.history.application.dto.*
import com.hub.history.domain.model.*
import com.hub.history.domain.repository.IncidentDetectionRepository
import com.hub.history.domain.repository.IncidentReviewRepository
import com.hub.population.domain.model.ResidentId
import com.hub.residence.domain.model.BedId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class IncidentApplicationService(
    private val detectionRepository: IncidentDetectionRepository,
    private val reviewRepository: IncidentReviewRepository
) {

    @Transactional
    fun ingestIncident(request: IngestIncidentRequest): IncidentDetectionResponse {
        val detection = IncidentDetection.create(
            sourceRecordId = request.sourceRecordId,
            residentId = ResidentId(request.residentId),
            bedId = request.bedId?.let { BedId(it) },
            kind = request.kind,
            severity = request.severity,
            occurredAt = request.occurredAt,
            source = request.source
        )
        return detectionRepository.save(detection).toResponse()
    }

    @Transactional(readOnly = true)
    fun getResidentIncidents(residentId: String): List<IncidentDetectionResponse> {
        return detectionRepository.findByResidentId(ResidentId(residentId)).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getIncidentSequence(incidentId: String): List<IncidentReviewResponse> {
        return reviewRepository.findByIncidentId(IncidentId(incidentId)).map { it.toReviewResponse() }
    }

    @Transactional
    fun reviewIncident(incidentId: String, request: ReviewIncidentRequest): IncidentReviewResponse {
        val review = IncidentReview.create(IncidentId(incidentId), request.actorId)
        return reviewRepository.save(review).toReviewResponse()
    }

    private fun IncidentDetection.toResponse() = IncidentDetectionResponse(
        id = id.value, sourceRecordId = sourceRecordId, residentId = residentId.value,
        bedId = bedId?.value, kind = kind, severity = severity, occurredAt = occurredAt,
        narrative = narrative, source = source
    )

    private fun IncidentReview.toReviewResponse() = IncidentReviewResponse(
        id = id.value, incidentId = incidentId.value, status = status,
        detectionVerdict = detectionVerdict, reviewNote = reviewNote,
        resolvedAt = resolvedAt, actorId = actorId
    )
}
