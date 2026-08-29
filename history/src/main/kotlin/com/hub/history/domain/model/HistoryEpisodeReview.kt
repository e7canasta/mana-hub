package com.hub.history.domain.model

import com.hub.shared.domain.AggregateRoot
import java.time.Instant

class HistoryEpisodeReview private constructor(
    override val id: HistoryEpisodeId,
    val episodeId: HistoryEpisodeId,
    val status: String,
    val detectionVerdict: String?,
    val reviewNote: String?,
    val resolvedAt: Instant?,
    val actorId: String,
    override var version: Long
) : AggregateRoot<HistoryEpisodeId>() {

    companion object {
        fun create(
            episodeId: HistoryEpisodeId,
            actorId: String,
            status: String = "pending",
            detectionVerdict: String? = null,
            reviewNote: String? = null,
        ): HistoryEpisodeReview = HistoryEpisodeReview(
            id = HistoryEpisodeId.random(), episodeId = episodeId, status = status,
            detectionVerdict = detectionVerdict, reviewNote = reviewNote,
            resolvedAt = if (status == "resolved") Instant.now() else null,
            actorId = actorId, version = 0
        )

        fun reconstitute(
            id: HistoryEpisodeId, episodeId: HistoryEpisodeId, status: String, detectionVerdict: String?,
            reviewNote: String?, resolvedAt: Instant?, actorId: String, version: Long
        ): HistoryEpisodeReview = HistoryEpisodeReview(id, episodeId, status, detectionVerdict, reviewNote, resolvedAt, actorId, version)
    }
}
