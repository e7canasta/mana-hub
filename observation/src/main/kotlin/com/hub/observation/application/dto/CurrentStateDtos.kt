package com.hub.observation.application.dto

import java.time.Instant

data class CurrentStateResponse(
    val residentId: String,
    val bedId: String?,
    val roomState: String?,
    val state: String?,
    val sleeping: Boolean?,
    val stateSince: Instant?
)
