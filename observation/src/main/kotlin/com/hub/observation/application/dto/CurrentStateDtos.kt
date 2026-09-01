package com.hub.observation.application.dto

import java.time.Instant

data class CurrentStateResponse(
    val residentId: String,
    val bedId: String? = null,
    val roomState: String? = null,
    val state: String? = null,
    val sleeping: Boolean? = null,
    val stateSince: Instant? = null,
    val staffPresent: Boolean? = null,
)
