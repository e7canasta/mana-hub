package com.hub.history.application.service

import com.hub.history.application.dto.*
import com.hub.history.domain.model.*
import com.hub.history.domain.repository.HistoryEpisodeDetectionRepository
import com.hub.history.domain.repository.HistoryEpisodeReviewRepository
import com.hub.population.domain.model.ResidentId
import com.hub.residence.domain.model.BedId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class HistoryEpisodeApplicationService(
    private val detectionRepository: HistoryEpisodeDetectionRepository,
    private val reviewRepository: HistoryEpisodeReviewRepository
) {

    @Transactional
    fun ingestHistoryEpisode(request: IngestHistoryEpisodeRequest): HistoryEpisodeResponse {
        val detection = HistoryEpisode.create(
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
    fun getResidentHistoryEpisodes(residentId: String): List<HistoryEpisodeResponse> {
        return detectionRepository.findByResidentId(ResidentId(residentId)).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getHistoryEpisodeSequence(episodeId: String): List<HistoryEpisodeReviewResponse> {
        return reviewRepository.findByEpisodeId(HistoryEpisodeId(episodeId)).map { it.toReviewResponse() }
    }

    @Transactional
    fun reviewHistoryEpisode(episodeId: String, request: ReviewHistoryEpisodeRequest): HistoryEpisodeReviewResponse {
        val review = HistoryEpisodeReview.create(
            episodeId = HistoryEpisodeId(episodeId),
            actorId = request.actorId,
            status = request.status,
            detectionVerdict = request.detectionVerdict,
            reviewNote = request.reviewNote,
        )
        return reviewRepository.save(review).toReviewResponse()
    }

    private fun HistoryEpisode.toResponse() = HistoryEpisodeResponse(
        id = id.value, sourceRecordId = sourceRecordId, residentId = residentId.value,
        bedId = bedId?.value, kind = kind, severity = severity, occurredAt = occurredAt,
        narrative = narrative, source = source
    )

    private fun HistoryEpisodeReview.toReviewResponse() = HistoryEpisodeReviewResponse(
        id = id.value, episodeId = episodeId.value, status = status,
        detectionVerdict = detectionVerdict, reviewNote = reviewNote,
        resolvedAt = resolvedAt, actorId = actorId
    )
}
