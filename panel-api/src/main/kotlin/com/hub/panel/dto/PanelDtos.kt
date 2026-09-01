package com.hub.panel.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.manahive.contracts.policy.MobilityAid
import com.manahive.contracts.policy.PolicyMode
import com.manahive.contracts.policy.RiskLevel

// ══════════════════════════════════════════════════════════════
//  Enums que YA EXISTEN en contracts — se reusan, no se duplican
//  RiskLevel, MobilityAid, PolicyMode → com.manahive.contracts.policy
// ══════════════════════════════════════════════════════════════

// Enums del panel que NO existen en contracts (específicos del SOR)
enum class EpisodeSeverity(val value: String) {
    INFO("info"), WARNING("warning"), CRITICAL("critical"), EMERGENCY("emergency");
    companion object { fun from(v: String) = entries.firstOrNull { it.value == v } ?: WARNING }
}

enum class EpisodeVerdict(val value: String) {
    CONFIRMED("confirmed"), NEAR_MISS("near_miss"), FALSE_POSITIVE("false_positive");
    companion object { fun from(v: String) = entries.firstOrNull { it.value == v } ?: CONFIRMED }
}

enum class AlarmAction(val value: String) {
    OFF("off"), NOTIFY("notify"), ALARM("alarm");
    companion object { fun from(v: String) = entries.firstOrNull { it.value == v } ?: OFF }
}

enum class NoteKind(val value: String) {
    CARE("CARE"), CLINICAL("CLINICAL"), INSIGHT("INSIGHT"), PATTERN("PATTERN"),
    OBSERVATION("OBSERVATION"), SUMMARY("SUMMARY"), RECOMMENDATION("RECOMMENDATION"),
    CLINICAL_NOTE("CLINICAL_NOTE"), ACKNOWLEDGEMENT("ACKNOWLEDGEMENT"), RESOLUTION("RESOLUTION");
    companion object { fun from(v: String) = entries.firstOrNull { it.value == v } ?: CARE }
}

enum class TransitionId(val value: String) {
    FALL("fall"), BED_EXIT("bed_exit"), WHEELCHAIR_EXIT("wheelchair_exit"),
    BATHROOM_DWELL("bathroom_dwell"), ROOM_EXIT("room_exit"), SLEEP_DWELL("sleep_dwell"),
    BED_RAIL("bed_rail"), WALKER_AID("walker_aid");
    companion object { fun from(v: String) = entries.firstOrNull { it.value == v } ?: FALL }
}

enum class TransitionGroup(val value: String) {
    FALL_PREVENTION("fall_prevention"), LOCATION("location"),
    SLEEP("sleep"), ENVIRONMENT("environment");
    companion object { fun from(v: String) = entries.firstOrNull { it.value == v } ?: FALL_PREVENTION }
}

enum class Shift(val value: String) {
    DAY("day"), NIGHT("night");
    companion object { fun from(v: String) = entries.firstOrNull { it.value == v } ?: DAY }
}

enum class EpisodeStatus(val value: String) {
    PENDING("pending"), RESOLVED("resolved");
    companion object { fun from(v: String) = entries.firstOrNull { it.value == v } ?: PENDING }
}

// ══════════════════════════════════════════════════════════════
//  Ubicación y estado
// ══════════════════════════════════════════════════════════════

data class LocationDto(
    @JsonProperty("wingName") val wingName: String?,
    @JsonProperty("roomNumber") val roomNumber: String?,
    @JsonProperty("bedLabel") val bedLabel: String?,
)

data class CurrentStateDto(
    val state: String?,
    @JsonProperty("staffPresent") val staffPresent: Boolean?,
    @JsonProperty("stateSince") val stateSince: String?,
)

// ══════════════════════════════════════════════════════════════
//  RESIDENTES
// ══════════════════════════════════════════════════════════════

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ResidentRailDto(
    val id: String,
    @JsonProperty("fullName") val fullName: String,
    val location: LocationDto?,
    @JsonProperty("currentState") val currentState: CurrentStateDto?,
)

// ══════════════════════════════════════════════════════════════
//  EPISODIOS — Feed
// ══════════════════════════════════════════════════════════════

data class EpisodeFeedDto(
    val episodes: List<EpisodeListItemDto>,
    val summary: EpisodeSummaryDto,
)

data class EpisodeListItemDto(
    val id: String,
    @JsonProperty("residentId") val residentId: String,
    @JsonProperty("residentName") val residentName: String,
    val location: LocationDto?,
    val severity: EpisodeSeverity,
    val kind: String,
    val title: String,
    @JsonProperty("openedAt") val openedAt: String,
    @JsonProperty("closedAt") val closedAt: String?,
    val verdict: EpisodeVerdict?,
    val injury: String?,
)

data class EpisodeSummaryDto(
    val pending: Int,
    val injured: Int,
    val total: Int,
)

// ══════════════════════════════════════════════════════════════
//  EPISODIOS — Detalle
// ══════════════════════════════════════════════════════════════

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EpisodeDetailDto(
    val id: String,
    @JsonProperty("residentId") val residentId: String,
    @JsonProperty("residentName") val residentName: String,
    val severity: EpisodeSeverity,
    val status: EpisodeStatus,
    val kind: String,
    val title: String?,
    val detail: String?,
    val narrative: String?,
    @JsonProperty("occurredAt") val occurredAt: String,
    @JsonProperty("closedAt") val closedAt: String?,
    val injury: String?,
    @JsonProperty("selfRecovery") val selfRecovery: Boolean?,
    @JsonProperty("responseSeconds") val responseSeconds: Int?,
    @JsonProperty("escalationLevel") val escalationLevel: Int?,
    val timeline: List<TimelineItemDto>,
    val reviews: List<ReviewDto>,
    val interventions: List<InterventionDto>,
    val notes: List<NoteDto>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TimelineItemDto(
    val at: String,
    val type: String,
    val eventType: String? = null,
    val from: String? = null,
    val to: String? = null,
    val signalType: String? = null,
    val severity: EpisodeSeverity? = null,
    @JsonProperty("ruleId") val ruleId: String? = null,
    val cause: String? = null,
    @JsonProperty("noteKind") val noteKind: NoteKind? = null,
    val body: String? = null,
    @JsonProperty("authorName") val authorName: String? = null,
    @JsonProperty("interventionKind") val interventionKind: String? = null,
    @JsonProperty("performedBy") val performedBy: String? = null,
    val detail: String? = null,
    val verdict: EpisodeVerdict? = null,
    @JsonProperty("reviewNote") val reviewNote: String? = null,
    val description: String? = null,
)

data class ReviewDto(
    val id: String,
    @JsonProperty("actorId") val actorId: String?,
    val verdict: EpisodeVerdict?,
    @JsonProperty("reviewNote") val reviewNote: String?,
    @JsonProperty("reviewedAt") val reviewedAt: String?,
)

data class InterventionDto(
    val kind: String,
    @JsonProperty("performedAt") val performedAt: String,
    @JsonProperty("performedBy") val performedBy: String?,
    val detail: String?,
)

data class NoteDto(
    val id: String,
    val kind: NoteKind,
    val body: String,
    @JsonProperty("authorId") val authorId: String,
    @JsonProperty("authorName") val authorName: String?,
    @JsonProperty("createdAt") val createdAt: String,
)

// ══════════════════════════════════════════════════════════════
//  COMANDOS — Requests
// ══════════════════════════════════════════════════════════════

data class ReviewEpisodeRequest(
    @JsonProperty("verdict") val verdict: EpisodeVerdict,
    @JsonProperty("note") val note: String? = null,
    @JsonProperty("actorId") val actorId: String,
)

data class CreateEpisodeNoteRequest(
    @JsonProperty("kind") val kind: NoteKind,
    @JsonProperty("body") val body: String,
    @JsonProperty("authorId") val authorId: String,
)

data class CreateResidentNoteRequest(
    @JsonProperty("kind") val kind: NoteKind,
    @JsonProperty("body") val body: String,
    @JsonProperty("authorId") val authorId: String,
)

// ══════════════════════════════════════════════════════════════
//  COMANDOS — Responses
// ══════════════════════════════════════════════════════════════

data class ReviewEpisodeResponse(
    val id: String,
    @JsonProperty("episodeId") val episodeId: String,
    val verdict: EpisodeVerdict,
    @JsonProperty("reviewNote") val reviewNote: String?,
    @JsonProperty("reviewedBy") val reviewedBy: String,
    @JsonProperty("reviewedAt") val reviewedAt: String,
)

data class NoteCreatedResponse(
    val id: String,
    @JsonProperty("episodeId") val episodeId: String?,
    @JsonProperty("residentId") val residentId: String?,
    val kind: NoteKind,
    val body: String,
    @JsonProperty("authorId") val authorId: String,
    @JsonProperty("createdAt") val createdAt: String,
)

// ══════════════════════════════════════════════════════════════
//  PREFERENCIAS
// ══════════════════════════════════════════════════════════════

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PreferenceFullDto(
    @JsonProperty("residentId") val residentId: String,
    @JsonProperty("residentName") val residentName: String,
    val location: LocationDto?,
    @JsonProperty("riskLevel") val riskLevel: RiskLevel,
    @JsonProperty("mobilityAid") val mobilityAid: MobilityAid,
    val autopilot: Boolean,
    val mode: PolicyMode,
    @JsonProperty("templateId") val templateId: String?,
    val overrides: Map<TransitionId, TransitionOverrideDto>,
    val recommendation: RecommendationDto?,
    @JsonProperty("updatedAt") val updatedAt: String?,
    @JsonProperty("updatedBy") val updatedBy: String?,
)

data class TransitionOverrideDto(
    @JsonProperty("day") val day: AlarmAction?,
    @JsonProperty("night") val night: AlarmAction?,
    @JsonProperty("warningMinutes") val warningMinutes: Int?,
    @JsonProperty("alertMinutes") val alertMinutes: Int?,
)

data class RecommendationDto(
    val changed: Boolean,
    val level: RiskLevel,
    val factors: List<String>,
    val score: Int?,
    @JsonProperty("signalsEvaluated") val signalsEvaluated: Int?,
    @JsonProperty("suggestedTemplate") val suggestedTemplate: String?,
    @JsonProperty("computedAt") val computedAt: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PreferenceListItemDto(
    @JsonProperty("residentId") val residentId: String,
    @JsonProperty("residentName") val residentName: String,
    val location: LocationDto?,
    @JsonProperty("riskLevel") val riskLevel: RiskLevel,
    @JsonProperty("mobilityAid") val mobilityAid: MobilityAid,
    val autopilot: Boolean,
    val mode: PolicyMode,
    @JsonProperty("dayActiveCount") val dayActiveCount: Int,
    @JsonProperty("nightActiveCount") val nightActiveCount: Int,
    val recommendation: RecommendationDto?,
    @JsonProperty("updatedAt") val updatedAt: String?,
    @JsonProperty("updatedBy") val updatedBy: String?,
)

data class SavePreferencesRequest(
    @JsonProperty("riskLevel") val riskLevel: RiskLevel? = null,
    @JsonProperty("mobilityAid") val mobilityAid: MobilityAid? = null,
    @JsonProperty("autopilot") val autopilot: Boolean? = null,
    @JsonProperty("mode") val mode: PolicyMode? = null,
    @JsonProperty("templateId") val templateId: String? = null,
    @JsonProperty("overrides") val overrides: Map<TransitionId, TransitionOverrideDto>? = null,
    @JsonProperty("reason") val reason: String? = null,
    @JsonProperty("updatedBy") val updatedBy: String? = null,
)

data class SavePreferencesResponse(
    val id: String,
    @JsonProperty("residentId") val residentId: String,
    @JsonProperty("riskLevel") val riskLevel: RiskLevel,
    @JsonProperty("mobilityAid") val mobilityAid: MobilityAid,
    val autopilot: Boolean,
    @JsonProperty("updatedAt") val updatedAt: String,
)

data class ResidentNotesResponse(val notes: List<NoteDto>)
data class EpisodeNotesResponse(val notes: List<NoteDto>)
data class EpisodeInterventionsResponse(val interventions: List<InterventionDto>)
data class RecommendationListResponse(val recommendations: List<RecommendationActionDto>)

data class RecommendationActionDto(
    val id: String,
    @JsonProperty("episodeId") val episodeId: String?,
    val title: String?,
    val description: String?,
    val state: String?,
    @JsonProperty("createdAt") val createdAt: String?,
)

// ══════════════════════════════════════════════════════════════
//  CATÁLOGO
// ══════════════════════════════════════════════════════════════

data class AlarmCatalogDto(
    val levels: List<RiskLevel>,
    @JsonProperty("mobilityAids") val mobilityAids: List<MobilityAid>,
    val transitions: List<TransitionCatalogDto>,
    val presets: Map<RiskLevel, Map<TransitionId, ShiftPresetDto>>,
    @JsonProperty("riskFactors") val riskFactors: List<RiskFactorCatalogDto>,
)

data class TransitionCatalogDto(
    val id: TransitionId,
    val group: TransitionGroup,
    val label: String,
    @JsonProperty("shortLabel") val shortLabel: String,
    val detail: String,
    val icon: String?,
    val locked: Boolean,
    @JsonProperty("requiresAid") val requiresAid: MobilityAid?,
    val params: List<ParamCatalogDto>,
)

data class ParamCatalogDto(
    val id: String,
    val type: String,
    val label: String,
    val unit: String?,
    val min: Int?,
    val max: Int?,
    val step: Int?,
)

data class ShiftPresetDto(
    val day: AlarmAction,
    val night: AlarmAction,
)

data class RiskFactorCatalogDto(
    val id: String,
    val label: String,
    val icon: String?,
)
