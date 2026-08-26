package com.hub.observation.domain.model

import com.hub.shared.domain.Identifier
import com.hub.population.domain.model.ResidentId
import com.hub.residence.domain.model.BedId
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
    val payloadJson: String
)
