package com.hub.clients.evidence

import com.hub.clients.core.EvidenceDsl
import com.hub.clients.core.HttpApi
import java.time.Instant

@EvidenceDsl
class EvidenceScope internal constructor(private val http: HttpApi) {

    fun createEvidence(bedId: String, residentId: String, evidenceType: String, category: String? = null, episodeId: String? = null): EvidenceResponse =
        http.post(
            "/api/v1/evidence",
            CreateEvidenceRequest(bedId, residentId, evidenceType, category, Instant.now(), episodeId),
            EvidenceResponse::class.java
        )

    fun evidenceByEpisode(episodeId: String): List<EvidenceResponse> =
        try { http.get("/api/v1/evidence?episodeId=$episodeId", Array<EvidenceResponse>::class.java).toList() }
        catch (_: Exception) { emptyList() }

    fun openTimeline(bedId: String, residentId: String): TimelineResponse =
        http.post("/api/v1/timelines?bedId=$bedId&residentId=$residentId", emptyMap<String, String>(), TimelineResponse::class.java)

    fun closeTimeline(timelineId: String): TimelineResponse =
        http.post("/api/v1/timelines/$timelineId/close", emptyMap<String, String>(), TimelineResponse::class.java)

    fun openClipWindow(bedId: String, residentId: String): ClipWindowResponse =
        http.post("/api/v1/clip-windows?bedId=$bedId&residentId=$residentId", emptyMap<String, String>(), ClipWindowResponse::class.java)

    fun closeClipWindow(windowId: String): ClipWindowResponse =
        http.post("/api/v1/clip-windows/$windowId/close", emptyMap<String, String>(), ClipWindowResponse::class.java)

    fun openClipWindows(bedId: String): List<ClipWindowResponse> =
        http.get("/api/v1/clip-windows/$bedId/open", Array<ClipWindowResponse>::class.java).toList()
}
