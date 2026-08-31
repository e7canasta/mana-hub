package com.hub.insights.api

import com.hub.insights.derive.SleepDerived
import com.hub.insights.find.Finding
import com.hub.insights.find.KpiCard
import com.hub.insights.inbound.BathroomSummaryData
import com.hub.insights.inbound.CareSummaryData
import com.hub.insights.inbound.MobilitySummaryData
import com.hub.insights.inbound.SleepSummaryData
import com.hub.insights.recommend.Recommendation
import java.time.LocalTime

data class SleepInsightResponse(
    val residentId: String,
    val from: String,
    val to: String,
    val observedFrom: String,
    val baselineReady: Boolean,
    val observedDays: Int,
    val summaries: List<SleepDayDto>,
    val derived: SleepDerivedDto,
    val cards: List<KpiCard> = emptyList(),
    val narrative: String? = null,
    val findings: List<Finding> = emptyList(),
    val recommendations: List<Recommendation>,
)

data class SleepDayDto(
    val day: String,
    val calmMinutes: Int,
    val restlessMinutes: Int,
    val awakeMinutes: Int,
    val outOfBedMinutes: Int,
    val bedExitCount: Int,
    val wakeCount: Int,
    val startedAt: String?,
    val endedAt: String?,
    val measured: Boolean,
)

data class SleepDerivedDto(
    val avgCalmMinutes7d: Int?,
    val deltaCalmMinutesWoW: Int?,
    val avgRestlessMinutes7d: Int?,
    val avgAsleepMinutes7d: Int?,
    val restlessShare: Double?,
    val avgBedExits: Double?,
    val maxBedExits: Int?,
    val avgTimeInBedMinutes: Int?,
    val sleepEfficiency: Double?,
    val habitualFrom: LocalTime?,
    val habitualTo: LocalTime?,
)

data class CareInsightResponse(
    val residentId: String,
    val from: String,
    val to: String,
    val observedFrom: String,
    val baselineReady: Boolean,
    val observedDays: Int,
    val summaries: List<CareDayDto>,
    val avgMinutesPerDay: Double?,
    val proactiveShare: Double?,
    val recommendations: List<Recommendation>,
)

data class CareDayDto(
    val day: String,
    val totalMinutes: Int,
    val proactiveMinutes: Int,
    val roundsCount: Int,
    val notesCount: Int,
    val measured: Boolean,
)

data class MobilityInsightResponse(
    val residentId: String,
    val from: String,
    val to: String,
    val observedFrom: String,
    val baselineReady: Boolean,
    val summaries: List<MobilityDayDto>,
    val avgWalkingMinutes: Int?,
    val estimatedDistanceMeters: Double?,
    val recommendations: List<Recommendation>,
)

data class MobilityDayDto(
    val day: String,
    val walkingMinutes: Int,
    val transferCount: Int,
    val outOfBedMinutes: Int,
    val inBedMinutes: Int,
    val outOfSightMinutes: Int,
    val measured: Boolean,
)

data class BathroomInsightResponse(
    val residentId: String,
    val from: String,
    val to: String,
    val observedFrom: String,
    val baselineReady: Boolean,
    val summaries: List<BathroomDayDto>,
)

data class BathroomDayDto(
    val day: String,
    val visitCount: Int,
    val nightVisitCount: Int,
    val assistedCount: Int,
    val totalMinutes: Int,
    val measured: Boolean,
)

data class RollupDayResult(
    val residentId: String,
    val observedOn: String,
    val skipped: Boolean = false,
    val reason: String? = null,
    val sleep: SleepSummaryData? = null,
    val mobility: MobilitySummaryData? = null,
    val bathroom: BathroomSummaryData? = null,
    val care: CareSummaryData? = null,
    val published: Map<String, String> = emptyMap(),
)

data class EpisodeResolvedRequest(
    val residentId: String,
    val episodeId: String? = null,
    val selfRecovery: Boolean = false,
    val durationMinutes: Int? = null,
)

fun SleepDerived.toDto() = SleepDerivedDto(
    avgCalmMinutes7d = avgCalmMinutes7d,
    deltaCalmMinutesWoW = deltaCalmMinutesWoW,
    avgRestlessMinutes7d = avgRestlessMinutes7d,
    avgAsleepMinutes7d = avgAsleepMinutes7d,
    restlessShare = restlessShare,
    avgBedExits = avgBedExits,
    maxBedExits = maxBedExits,
    avgTimeInBedMinutes = avgTimeInBedMinutes,
    sleepEfficiency = sleepEfficiency,
    habitualFrom = habitualFrom,
    habitualTo = habitualTo,
)
