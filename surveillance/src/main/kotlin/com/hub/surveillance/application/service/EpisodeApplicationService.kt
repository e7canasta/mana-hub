package com.hub.surveillance.application.service

import com.hub.surveillance.application.dto.*
import com.hub.surveillance.domain.model.*
import com.hub.surveillance.domain.repository.EpisodeRepository
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.BedId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class EpisodeApplicationService(
    private val episodeRepository: EpisodeRepository
) {

    @Transactional
    fun createEpisode(request: CreateEpisodeRequest): EpisodeResponse {
        val episode = Episode.create(
            residentId = ResidentId(request.residentId),
            bedId = request.bedId?.let { BedId(it) },
            severity = request.severity,
            title = request.title,
            detail = request.detail,
            occurredAt = request.occurredAt,
            evidenceKind = request.evidenceKind,
            evidenceRef = request.evidenceRef
        )
        return episodeRepository.save(episode).toResponse()
    }

    @Transactional(readOnly = true)
    fun listEpisodes(
        residentId: String? = null,
        status: String? = null,
        from: String? = null,
        to: String? = null
    ): List<EpisodeResponse> {
        val fromInstant = from?.let { Instant.parse(it) }
        val toInstant = to?.let { Instant.parse(it) }
        return episodeRepository.findFiltered(
            residentId = residentId?.let { ResidentId(it) },
            status = status,
            from = fromInstant,
            to = toInstant
        ).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getEpisode(episodeId: String): EpisodeResponse? {
        return episodeRepository.findById(EpisodeId(episodeId))?.toResponse()
    }

    @Transactional
    fun acknowledgeEpisode(episodeId: String, actorId: String): EpisodeResponse {
        val episode = episodeRepository.findById(EpisodeId(episodeId))
            ?: throw IllegalArgumentException("Episode not found: $episodeId")
        return episodeRepository.save(episode.acknowledge(actorId)).toResponse()
    }

    @Transactional
    fun updateEpisode(episodeId: String, request: UpdateEpisodeRequest): EpisodeResponse {
        val episode = episodeRepository.findById(EpisodeId(episodeId))
            ?: throw IllegalArgumentException("Episode not found: $episodeId")
        return episodeRepository.save(episode.resolve(request.status ?: "resolved")).toResponse()
    }

    private fun Episode.toResponse() = EpisodeResponse(
        id = id.value, residentId = residentId.value, bedId = bedId?.value, severity = severity,
        status = status, title = title, detail = detail, occurredAt = occurredAt,
        escalationLevel = escalationLevel, isPending = isPending
    )
}
