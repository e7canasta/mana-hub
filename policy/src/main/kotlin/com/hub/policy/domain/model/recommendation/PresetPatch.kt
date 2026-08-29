package com.hub.policy.domain.model.recommendation

import com.hub.policy.domain.model.TemplateId
import com.hub.policy.domain.model.PolicyMode
import com.hub.policy.domain.model.MobilityAid
import com.hub.policy.domain.model.RiskLevel

/**
 * Parche parcial sobre el preset de alarma del residente.
 * Cada campo null significa "no toco este valor".
 * Solo se sobreescribe lo que está definido.
 */
data class PresetPatch(
    val templateId: TemplateId? = null,
    val mode: PolicyMode? = null,
    val riskLevel: RiskLevel? = null,
    val mobilityAid: MobilityAid? = null,
    val autopilot: Boolean? = null,
)
