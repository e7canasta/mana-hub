package com.hub.clients.history

import com.hub.clients.core.HistoryDsl
import com.hub.clients.core.HttpApi

@HistoryDsl
class HistoryScope internal constructor(private val http: HttpApi) {

    fun residentHistoryEpisodes(residentId: String): List<HistoryEpisodeResponse> =
        http.get("/api/v1/residents/$residentId/history-episodes", Array<HistoryEpisodeResponse>::class.java).toList()

    fun historyEpisodeSequence(episodeId: String): List<HistoryEpisodeReviewResponse> =
        http.get("/api/v1/history-episodes/$episodeId/sequence", Array<HistoryEpisodeReviewResponse>::class.java).toList()

    fun reviewHistoryEpisode(episodeId: String, status: String, actorId: String, verdict: String? = null, note: String? = null): HistoryEpisodeReviewResponse =
        http.patch(
            "/api/v1/history-episodes/$episodeId",
            ReviewHistoryEpisodeRequest(status, verdict, note, actorId),
            HistoryEpisodeReviewResponse::class.java
        )
}
