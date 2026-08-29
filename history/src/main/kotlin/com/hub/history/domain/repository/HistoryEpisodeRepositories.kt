package com.hub.history.domain.repository

import com.hub.history.domain.model.*
import com.hub.shared.domain.ResidentId

interface HistoryEpisodeDetectionRepository {
    fun findById(id: HistoryEpisodeId): HistoryEpisode?
    fun findBySourceRecordId(sourceRecordId: String): HistoryEpisode?
    fun findByResidentId(residentId: ResidentId): List<HistoryEpisode>
    fun findByResidentIdAndKind(residentId: ResidentId, kind: EpisodeKind): List<HistoryEpisode>
    fun save(detection: HistoryEpisode): HistoryEpisode
}

interface HistoryEpisodeReviewRepository {
    fun findByEpisodeId(episodeId: HistoryEpisodeId): List<HistoryEpisodeReview>
    fun save(review: HistoryEpisodeReview): HistoryEpisodeReview
}

interface HistoryEpisodeInterventionRepository {
    fun findByEpisodeId(episodeId: HistoryEpisodeId): List<HistoryEpisodeIntervention>
    fun save(intervention: HistoryEpisodeIntervention): HistoryEpisodeIntervention
    fun saveAll(interventions: List<HistoryEpisodeIntervention>): List<HistoryEpisodeIntervention>
    fun deleteByEpisodeId(episodeId: HistoryEpisodeId)
}
