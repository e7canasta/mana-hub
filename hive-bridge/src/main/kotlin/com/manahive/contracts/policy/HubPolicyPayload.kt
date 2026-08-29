package com.manahive.contracts.policy

import java.time.Instant

// Shared typed enums — mirror hub's RiskLevel/MobilityAid/PolicyMode y hive's WatchLevel
// Tipado para que el puente no sea JSON sin contrato (Fowler: "Published Language")
enum class HubRiskLevel { LOW, MEDIUM, HIGH }
enum class HubMobilityAid { NONE, WALKER, WHEELCHAIR }
enum class HubPolicyMode { PRESET, CUSTOM }

/**
 * Payload tipado publicado por mana-hub a hub.policy.change.v1 y hub.policy.effective-rules.v1.<resident>
 * Reutiliza EventEnvelope + Subjects compartidos. Tipado evita que hive reciba "fall_risk" vs "FALL_RISK".
 */
data class HubPolicyChange(
    val residentId: String,
    val at: Instant,
    val riskLevel: HubRiskLevel,
    val templateId: String?,
    val mobilityAid: HubMobilityAid?,
    val autopilot: Boolean,
    val mode: HubPolicyMode?,
    val overridesJson: String,
    val fingerprint: String,
    val source: String = "mana-hub"
)
