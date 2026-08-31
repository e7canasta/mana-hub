package com.hub.surveillance.application.service

import com.hub.surveillance.application.dto.*
import com.hub.surveillance.domain.model.*
import com.hub.surveillance.domain.repository.EpisodeRepository
import com.hub.shared.domain.DomainEventPublisher
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.BedId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class EpisodeApplicationService(
    private val episodeRepository: EpisodeRepository,
    private val eventPublisher: DomainEventPublisher
) {

    @Transactional
    fun createEpisode(request: CreateEpisodeRequest): EpisodeResponse {
        if (request.id != null) {
            val existing = episodeRepository.findById(EpisodeId.from(request.id))
            if (existing != null) {
                return existing.toResponse()
            }
        }

        return try {
            val episode = Episode.create(
                residentId = ResidentId(request.residentId),
                bedId = request.bedId?.let { BedId(it) },
                severity = request.severity,
                title = request.title,
                detail = request.detail,
                occurredAt = request.occurredAt,
                evidenceKind = request.evidenceKind,
                evidenceRef = request.evidenceRef,
                id = request.id?.let { EpisodeId.from(it) },
            )
            val saved = episodeRepository.save(episode)
            publishEvents(episode)
            saved.toResponse()
        } catch (e: org.springframework.dao.DataIntegrityViolationException) {
            if (request.id != null) {
                episodeRepository.findById(EpisodeId.from(request.id))?.toResponse()
                    ?: throw e
            } else throw e
        }
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
        val updated = episode.acknowledge(actorId)
        val saved = episodeRepository.save(updated)
        publishEvents(updated)
        return saved.toResponse()
    }

    @Transactional
    fun updateEpisode(episodeId: String, request: UpdateEpisodeRequest): EpisodeResponse {
        val episode = episodeRepository.findById(EpisodeId(episodeId))
            ?: throw IllegalArgumentException("Episode not found: $episodeId")
        val updated = episode.resolve(request.status ?: "resolved")
        val saved = episodeRepository.save(updated)
        publishEvents(updated)
        return saved.toResponse()
    }

    private fun publishEvents(episode: Episode) {
        episode.domainEvents.forEach { eventPublisher.publish(it) }
        episode.clearEvents()
    }

    private fun Episode.toResponse() = EpisodeResponse(
        id = id.value, residentId = residentId.value, bedId = bedId?.value, severity = severity,
        status = status, title = title, detail = detail, occurredAt = occurredAt,
        escalationLevel = escalationLevel, isPending = isPending
    )
}
