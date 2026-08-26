package com.hub.clients.history

import com.hub.clients.core.HistoryDsl
import com.hub.clients.core.HttpApi

@HistoryDsl
class HistoryScope internal constructor(private val http: HttpApi) {

    fun residentIncidents(residentId: String): List<IncidentDetectionResponse> =
        http.get("/api/v1/residents/$residentId/incidents", Array<IncidentDetectionResponse>::class.java).toList()

    fun incidentSequence(incidentId: String): List<IncidentReviewResponse> =
        http.get("/api/v1/incidents/$incidentId/sequence", Array<IncidentReviewResponse>::class.java).toList()

    fun reviewIncident(incidentId: String, status: String, actorId: String, verdict: String? = null, note: String? = null): IncidentReviewResponse =
        http.patch(
            "/api/v1/incidents/$incidentId",
            ReviewIncidentRequest(status, verdict, note, actorId),
            IncidentReviewResponse::class.java
        )
}
