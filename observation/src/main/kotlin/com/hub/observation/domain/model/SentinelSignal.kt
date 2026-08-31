package com.hub.observation.domain.model

import com.hub.shared.domain.BedId
import com.hub.shared.domain.Identifier
import com.hub.shared.domain.ResidentId
import java.time.Instant

/**
 * Sentinel signal persisted as audit — the origin of episodes.
 * Every EPISODE_OPENED/CLOSED/COMPLICATED is stored before episode mutation.
 * Fowler: Domain Event, not DTO.
 */
data class SentinelSignal(
    val id: Identifier,
    val signalId: String,
    val bedId: BedId,
    val residentId: ResidentId?,
    val episodeId: String?,
    val type: String,
    val severity: String?,
    val trigger: String?,
    val timestamp: Instant,
    val payloadJson: String,
)
