package com.hub.integration.signal

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class SignalEnvelope(
    val type: String? = null,
    val severity: String? = null,
    val residentId: String? = null,
    val bedId: String? = null,
    val monitorKey: String? = null,
    val timestamp: String? = null,

    @JsonProperty("from_state")
    val fromState: String? = null,
    @JsonProperty("to_state")
    val toState: String? = null,
    @JsonProperty("trigger_type")
    val triggerType: String? = null,

    val episode: String? = null,
    val rule: String? = null,
    val trigger: String? = null,
    val resident: String? = null,
    val field: String? = null,
    val cause: String? = null,
    val state: String? = null,
    val baseline: String? = null,

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

    val detail: String? = null,
)
