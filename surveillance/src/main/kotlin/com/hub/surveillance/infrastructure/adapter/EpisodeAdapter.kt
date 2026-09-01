package com.hub.surveillance.infrastructure.adapter

import com.hub.shared.domain.port.*
import com.hub.surveillance.application.dto.CreateEpisodeRequest
import com.hub.surveillance.application.dto.UpdateEpisodeRequest
import com.hub.surveillance.application.service.EpisodeApplicationService
import com.hub.shared.domain.EpisodeId
import com.hub.surveillance.domain.model.EpisodeSeverity
import com.hub.surveillance.domain.repository.EpisodeRepository
import org.springframework.stereotype.Component

@Component
class EpisodeAdapter(
    private val episodeService: EpisodeApplicationService,
    private val episodeRepository: EpisodeRepository,
) : EpisodePort {
    override fun createEpisode(request: CreateEpisodePortRequest): EpisodePortResponse {
        val response = episodeService.createEpisode(CreateEpisodeRequest(
            id = request.id,
            residentId = request.residentId,
            bedId = request.bedId,
            severity = EpisodeSeverity.from(request.severity),
            title = request.title,
            detail = request.detail,
            occurredAt = request.occurredAt,
        ))
        return EpisodePortResponse(response.id)
    }

    override fun updateEpisode(episodeId: String, request: UpdateEpisodePortRequest) {
        episodeService.updateEpisode(episodeId, UpdateEpisodeRequest(status = request.status))
    }

    override fun findById(episodeId: String): EpisodePortModel? {
        return episodeRepository.findById(EpisodeId(episodeId))?.let {
            EpisodePortModel(id = it.id.value, severity = it.severity.name, escalationLevel = it.escalationLevel)
        }
    }

    override fun save(episode: EpisodePortModel) {
        // Episode domain model is reconstructed via findById + domain methods, not directly saved
    }
}
