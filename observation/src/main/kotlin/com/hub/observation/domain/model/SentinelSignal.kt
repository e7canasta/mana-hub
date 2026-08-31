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
    // V16 enrichment — desnormalizados de payload_jsonb (toMap) para reporte sin parsear JSON
    val ruleId: String? = null,
    val field: String? = null,
    val triggerOn: String? = null,
    val cause: String? = null,
    val state: String? = null,
    val baseline: String? = null,
    val rulesFingerprint: String? = null,
    val gapDuration: String? = null,
    val previousSeverity: String? = null,
    val originalSeverity: String? = null,
    // V17 — detalles de regla sin JSON repetido (payload ya trae reversible/requiresNvr/...)
    val reversible: Boolean? = null,
    val requiresNvr: Boolean? = null,
    val confirmationWindow: String? = null,
    val requiresConfirmation: Boolean? = null,
    val elapsed: String? = null,
    val threshold: String? = null,
)
