package com.hub.integration.port

import com.hub.shared.domain.BedId
import com.hub.shared.domain.Identifier
import com.hub.shared.domain.ResidentId
import java.time.Instant

interface SceneEventPort {
    fun save(event: SceneEventModel)
}

data class SceneEventModel(
    val id: Identifier,
    val eventId: String,
    val bedId: BedId,
    val residentId: ResidentId?,
    val eventType: String?,
    val fromState: String?,
    val toState: String?,
    val triggerType: String?,
    val timestamp: Instant,
    val payloadJson: String,
    val twinSnapshotJson: String,
    val stateSince: Instant?,
    val sceneSince: Instant?,
    val signalLost: Boolean?,
    val monitorId: String?,
)
