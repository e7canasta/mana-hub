package com.hub.insights.domain.find

import com.hub.insights.domain.derive.Baseline
import com.hub.insights.domain.derive.SleepDerived
import com.hub.insights.inbound.HubBathroomDay
import com.hub.insights.inbound.HubSleepDay
import java.time.Instant
import java.time.ZoneId

enum class FindingKind {
    TREND,
    CLUSTER,
    POLICY,
    WATCH,
    BRIEFING,
}

enum class Polarity {
    CONCERN,
    POSITIVE,
    NEUTRAL,
}

data class FindingProposal(
    val action: String,
    val text: String,
    val applyLabel: String = "Aplicar el cambio",
    val dismissLabel: String = "No hacerlo",
)

data class Finding(
    val code: String,
    val kind: FindingKind,
    val polarity: Polarity,
    val severity: String,
    val headline: String,
    val body: String,
    val windowDays: Int,
    val residentId: String,
    val residentName: String? = null,
    val awaitingDecision: Boolean = false,
    val evidence: Map<String, Any?> = emptyMap(),
    val proposal: FindingProposal? = null,
)

data class KpiCard(
    val code: String,
    val label: String,
    val value: String,
    val detail: String? = null,
)

data class FindingContext(
    val residentId: String,
    val residentName: String?,
    val baseline: Baseline,
    val sleep: SleepDerived,
    val sleepDays: List<HubSleepDay>,
    val bathroomDays: List<HubBathroomDay> = emptyList(),
    val careAvgMinutes: Double? = null,
    val exitsLast7d: List<Instant> = emptyList(),
    val staffAfterExitCount: Int = 0,
    val riskLevel: String? = null,
    val bedEdgeWarningMinutes: Int? = null,
    val zone: ZoneId,
    val windowDays: Int,
    val relatedEpisodeIds: List<String> = emptyList(),
)

data class EpisodeRef(
    val id: String,
    val kind: String,
    val severity: String? = null,
    val occurredAt: Instant,
    val selfRecovery: Boolean? = null,
)

data class ResidentBriefing(
    val residentId: String,
    val residentName: String?,
    val from: String,
    val to: String,
    val observedFrom: String,
    val baselineReady: Boolean,
    val observedDays: Int,
    val generatedAt: Instant,
    val policyToday: List<String>,
    val sleepCards: List<KpiCard>,
    val narrative: String?,
    val findings: List<Finding>,
    val downloadUrl: String? = null,
)

data class ResidentReport(
    val residentId: String,
    val residentName: String?,
    val from: String,
    val to: String,
    val observedFrom: String,
    val baselineReady: Boolean,
    val observedDays: Int,
    val generatedAt: Instant,
    val policyToday: List<String>,
    val sleepCards: List<KpiCard>,
    val narrative: String?,
    val findings: List<Finding>,
    val episodes: List<EpisodeRef>,
    val downloadUrl: String? = null,
)

data class ResidentFindingSummary(
    val residentId: String,
    val residentName: String?,
    val code: String,
    val kind: FindingKind,
    val polarity: Polarity,
    val severity: String,
    val headline: String,
    val awaitingDecision: Boolean = false,
)

data class FacilityBriefing(
    val generatedAt: Instant,
    val from: String,
    val to: String,
    val residentCount: Int,
    val baselineForming: Int,
    val toReview: List<ResidentFindingSummary>,
    val positive: List<ResidentFindingSummary>,
    val downloadUrl: String? = null,
)

data class FacilityReport(
    val generatedAt: Instant,
    val from: String,
    val to: String,
    val residentCount: Int,
    val baselineForming: Int,
    val toReview: List<ResidentFindingSummary>,
    val positive: List<ResidentFindingSummary>,
    val residents: List<ResidentReport>,
    val downloadUrl: String? = null,
)
