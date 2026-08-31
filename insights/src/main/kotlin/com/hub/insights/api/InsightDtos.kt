package com.hub.insights.api

import com.hub.insights.derive.SleepDerived
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
)

data class SleepDerivedDto(
    val avgCalmMinutes7d: Int?,
    val deltaCalmMinutesWoW: Int?,
    val avgRestlessMinutes7d: Int?,
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
)

data class MobilityInsightResponse(
    val residentId: String,
    val from: String,
    val to: String,
    val observedFrom: String,
    val baselineReady: Boolean,
    val summaries: List<MobilityDayDto>,
    val avgWalkingMinutes: Int?,
    val avgDistanceMeters: Double?,
    val recommendations: List<Recommendation>,
)

data class MobilityDayDto(
    val day: String,
    val walkingMinutes: Int,
    val distanceMeters: Double,
    val transferCount: Int,
    val outOfBedMinutes: Int,
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
)

data class RollupDayResult(
    val residentId: String,
    val observedOn: String,
    val skipped: Boolean = false,
    val reason: String? = null,
    val sleep: Map<String, Any?>? = null,
    val mobility: Map<String, Any?>? = null,
    val bathroom: Map<String, Any?>? = null,
    val care: Map<String, Any?>? = null,
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
    restlessShare = restlessShare,
    avgBedExits = avgBedExits,
    maxBedExits = maxBedExits,
    avgTimeInBedMinutes = avgTimeInBedMinutes,
    sleepEfficiency = sleepEfficiency,
    habitualFrom = habitualFrom,
    habitualTo = habitualTo,
)
