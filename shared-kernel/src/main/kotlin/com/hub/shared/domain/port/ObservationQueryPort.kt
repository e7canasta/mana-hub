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
    val twinSnapshotJson: String = "{}",
    val stateSince: Instant? = null,
    val sceneSince: Instant? = null,
    val signalLost: Boolean? = null,
    val monitorId: String? = null,
    val triggerType: String? = null,
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
    val baseline: String? = null,
    val rulesFingerprint: String? = null,
    val gapDuration: String? = null,
    val previousSeverity: String? = null,
    val originalSeverity: String? = null,
    val reversible: Boolean? = null,
    val requiresNvr: Boolean? = null,
    val confirmationWindow: String? = null,
    val requiresConfirmation: Boolean? = null,
    val elapsed: String? = null,
    val threshold: String? = null,
    val ruleId: String? = null,
    val field: String? = null,
)

interface ObservationQueryPort {
    fun findScenesByResidentId(residentId: ResidentId, from: Instant?, to: Instant?): List<SceneEventSnapshot>
    fun findScenesByBedId(bedId: String): List<SceneEventSnapshot>
    fun findSignalsByEpisodeId(episodeId: String): List<SentinelSignalSnapshot>
    fun findSignalsByResidentId(residentId: ResidentId, from: Instant?, to: Instant?): List<SentinelSignalSnapshot>
}
