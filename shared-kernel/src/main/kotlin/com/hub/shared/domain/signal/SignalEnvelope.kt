package com.hub.shared.domain.signal

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Typed envelope for sentinel signals arriving from mana-hive.
 * Replaces raw JsonNode field access with a validated data class.
 *
 * Canonical terms from vocabulario-unificado.md:
 *   type      → signal kind (EPISODE_OPENED, EPISODE_CLOSED, etc.)
 *   severity  → episode severity (INFO, WARNING, CRITICAL, EMERGENCY)
 *   fromState / toState → CAMBIO DE ESCENA transition (person state)
 *   trigger   → what caused the signal
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SignalEnvelope(
    // ── Core signal fields ──────────────────────────────────────────
    val type: String? = null,
    val severity: String? = null,
    val residentId: String? = null,
    val bedId: String? = null,
    val monitorKey: String? = null,
    val timestamp: String? = null,

    // ── Scene transition (CAMBIO DE ESCENA) ─────────────────────────
    @JsonProperty("from_state")
    val fromState: String? = null,
    @JsonProperty("to_state")
    val toState: String? = null,
    @JsonProperty("trigger_type")
    val triggerType: String? = null,

    // ── Lifecycle fields ────────────────────────────────────────────
    val episode: String? = null,
    val rule: String? = null,
    val trigger: String? = null,
    val resident: String? = null,
    val field: String? = null,
    val cause: String? = null,
    val state: String? = null,
    val baseline: String? = null,

    // ── Enrichment fields (V16/V17) ─────────────────────────────────
    @JsonProperty("triggerOn")
    val triggerOn: String? = null,
    @JsonProperty("rulesFingerprint")
    val rulesFingerprint: String? = null,
    @JsonProperty("gapDuration")
    val gapDuration: String? = null,
    @JsonProperty("previousSeverity")
    val previousSeverity: String? = null,
    @JsonProperty("originalSeverity")
    val originalSeverity: String? = null,
    val reversible: Boolean? = null,
    @JsonProperty("requiresNvr")
    val requiresNvr: Boolean? = null,
    @JsonProperty("confirmationWindow")
    val confirmationWindow: String? = null,
    @JsonProperty("requiresConfirmation")
    val requiresConfirmation: Boolean? = null,
    val elapsed: String? = null,
    val threshold: String? = null,

    // ── Raw fallback ────────────────────────────────────────────────
    val detail: String? = null,
)
