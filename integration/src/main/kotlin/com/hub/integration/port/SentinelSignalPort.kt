package com.hub.integration.port

import com.hub.shared.domain.BedId
import com.hub.shared.domain.Identifier
import com.hub.shared.domain.ResidentId
import java.time.Instant

interface SentinelSignalPort {
    fun save(signal: SentinelSignalModel)
}

data class SentinelSignalModel(
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
    val ruleId: String?,
    val field: String?,
    val triggerOn: String?,
    val cause: String?,
    val state: String?,
    val baseline: String?,
    val rulesFingerprint: String?,
    val gapDuration: String?,
    val previousSeverity: String?,
    val originalSeverity: String?,
    val reversible: Boolean?,
    val requiresNvr: Boolean?,
    val confirmationWindow: String?,
    val requiresConfirmation: Boolean?,
    val elapsed: String?,
    val threshold: String?,
)
