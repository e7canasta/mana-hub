package com.hub.shared.domain.port

import com.hub.shared.domain.ResidentId
import java.time.Instant

data class SceneEventSnapshot(
    val id: String,
    val bedId: String,
    val residentId: String?,
    val fromState: String?,
    val toState: String?,
    val eventType: String?,
    val observedAt: Instant,
    val confidence: Double?,
    val payloadJson: String? = null,
)

data class SentinelSignalSnapshot(
    val id: String,
    val bedId: String,
    val residentId: String?,
    val episodeId: String?,
    val signalType: String?,
    val severity: String?,
    val observedAt: Instant,
    val trigger: String? = null,
    val cause: String? = null,
    val state: String? = null,
    val triggerOn: String? = null,
    val payloadJson: String? = null,
)

interface ObservationQueryPort {
    fun findScenesByResidentId(residentId: ResidentId, from: Instant?, to: Instant?): List<SceneEventSnapshot>
    fun findScenesByBedId(bedId: String): List<SceneEventSnapshot>
    fun findSignalsByEpisodeId(episodeId: String): List<SentinelSignalSnapshot>
    fun findSignalsByResidentId(residentId: ResidentId, from: Instant?, to: Instant?): List<SentinelSignalSnapshot>
}
