package com.hub.observation.domain.model

import com.hub.shared.domain.Identifier
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.BedId
import java.time.Instant

data class SceneEvent(
    val id: Identifier,
    val eventId: String,
    val bedId: BedId,
    val residentId: ResidentId?,
    val eventType: String,
    val fromState: String?,
    val toState: String?,
    val triggerType: String?,
    val timestamp: Instant,
    val payloadJson: String,
    val twinSnapshotJson: String = "{}",
    val stateSince: Instant? = null,
    val sceneSince: Instant? = null,
    val signalLost: Boolean? = null,
    val monitorId: String? = null
)
