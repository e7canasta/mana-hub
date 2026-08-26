package com.hub.observation.domain.model

import com.hub.population.domain.model.ResidentId
import com.hub.residence.domain.model.BedId
import java.time.Instant

data class CurrentBedState(
    val bedId: BedId,
    val residentId: ResidentId?,
    val roomState: String?,
    val state: String?,
    val substate: String?,
    val sleeping: Boolean?,
    val stateSince: Instant,
    val updated: Instant,
    val source: String?,
    val sourceEventId: String?
)
