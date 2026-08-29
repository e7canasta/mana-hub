package com.hub.policy.domain.model

import com.hub.shared.domain.Identifier

sealed interface PolicyOverride {
    val id: Identifier
    val ruleId: String

    data class HysteresisOverride(
        override val id: Identifier,
        override val ruleId: String,
        val transitionKey: String,
        val hysteresisSeconds: Int
    ) : PolicyOverride

    data class DwellOverride(
        override val id: Identifier,
        override val ruleId: String,
        val stateKind: String,
        val warningAfterMinutes: Int?,
        val alertAfterMinutes: Int?
    ) : PolicyOverride

    data class ComeBackOverride(
        override val id: Identifier,
        override val ruleId: String,
        val baselineState: String,
        val warningAfterMinutes: Int?,
        val alertAfterMinutes: Int?,
        val severity: String?,
        val closureCondition: String?
    ) : PolicyOverride
}
