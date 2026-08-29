package com.hub.history.domain.repository

import com.hub.history.domain.model.*
import com.hub.population.domain.model.ResidentId

interface HistoryEpisodeDetectionRepository {
    fun findById(id: HistoryEpisodeId): HistoryEpisode?
    fun findBySourceRecordId(sourceRecordId: String): HistoryEpisode?
    fun findByResidentId(residentId: ResidentId): List<HistoryEpisode>
    fun save(detection: HistoryEpisode): HistoryEpisode
}

interface HistoryEpisodeReviewRepository {
    fun findByEpisodeId(episodeId: HistoryEpisodeId): List<HistoryEpisodeReview>
    fun save(review: HistoryEpisodeReview): HistoryEpisodeReview
}
