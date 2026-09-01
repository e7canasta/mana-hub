package com.hub.views.readers

import com.hub.history.domain.repository.HistoryEpisodeDetectionRepository
import com.hub.history.domain.repository.HistoryEpisodeReviewRepository
import com.hub.shared.domain.ResidentId
import com.hub.views.EpisodeListItemProjection
import com.hub.views.EpisodesTabProjection
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class EpisodesProjectionReader(
    private val historyEpisodeRepository: HistoryEpisodeDetectionRepository,
    private val historyReviewRepository: HistoryEpisodeReviewRepository,
) {

    @Transactional(readOnly = true)
    fun getEpisodesTab(residentId: String): EpisodesTabProjection {
        val episodes = historyEpisodeRepository.findByResidentId(ResidentId(residentId))
        val reviews = episodes.map { ep ->
            historyReviewRepository.findByEpisodeId(ep.id)
        }
        return EpisodesTabProjection(
            residentId = residentId,
            episodes = episodes.zip(reviews).map { (ep, revs) ->
                val lastReview = revs.maxByOrNull { it.resolvedAt ?: java.time.Instant.MIN }
                EpisodeListItemProjection(
                    id = ep.id.value,
                    kind = ep.kind.name,
                    severity = ep.severity.name,
                    occurredAt = ep.occurredAt,
                    injuryStatus = ep.injuryStatus,
                    selfRecovery = ep.selfRecovery,
                    verdict = lastReview?.detectionVerdict,
                    reviewNote = lastReview?.reviewNote,
                    reviewedAt = lastReview?.resolvedAt,
                )
            },
        )
    }
}
