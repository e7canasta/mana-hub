package com.hub.views

import java.time.Instant
import java.time.LocalDate

// ──────────────────────────────────────────────────────────── resident-rail

data class ResidentRailItem(
    val id: String,
    val fullName: String,
    val location: RailLocation?,
    val currentState: RailState?,
)

data class RailLocation(
    val wingName: String?,
    val roomNumber: String?,
    val bedLabel: String?,
)

data class RailState(
    val state: String?,
    val staffPresent: Boolean?,
    val stateSince: Instant?,
)

// ──────────────────────────────────────────────────────── resident-chart

data class ResidentChartProjection(
    val id: String,
    val fullName: String,
    val birthDate: LocalDate?,
    val admissionDate: LocalDate?,
    val location: RailLocation?,
    val currentState: RailState?,
)

// ───────────────────────────────────────────────────── sleep/mobility/bathroom

data class SleepTabProjection(
    val residentId: String,
    val from: String,
    val to: String,
    val summaries: List<SleepDayProjection>,
)

data class SleepDayProjection(
    val day: String,
    val calmMinutes: Int,
    val restlessMinutes: Int,
    val awakeMinutes: Int,
    val outOfBedMinutes: Int,
    val bedExitCount: Int,
    val wakeCount: Int,
    val startedAt: Instant?,
    val endedAt: Instant?,
)

data class MobilityTabProjection(
    val residentId: String,
    val from: String,
    val to: String,
    val summaries: List<MobilityDayProjection>,
)

data class MobilityDayProjection(
    val day: String,
    val walkingMinutes: Int,
    val distanceMeters: Double,
    val transferCount: Int,
    val outOfBedMinutes: Int,
)

data class BathroomTabProjection(
    val residentId: String,
    val from: String,
    val to: String,
    val summaries: List<BathroomDayProjection>,
)

data class BathroomDayProjection(
    val day: String,
    val visitCount: Int,
    val nightVisitCount: Int,
)

// ───────────────────────────────────────────────────────────── care

data class CareTabProjection(
    val residentId: String,
    val from: String,
    val to: String,
    val summaries: List<CareDayProjection>,
    val avgMinutesPerDay: Double?,
    val proactiveShare: Double?,
)

data class CareDayProjection(
    val day: String,
    val totalMinutes: Int,
    val proactiveMinutes: Int,
    val roundsCount: Int,
    val notesCount: Int,
)

// ───────────────────────────────────────────────────────────── falls

data class FallsTabProjection(
    val residentId: String,
    val streakDays: Int,
    val previousStreakDays: Int,
    val fallsLast12Months: Int,
    val lastFallAt: Instant?,
    val lastFallInjury: String?,
    val months: List<FallMonthProjection>,
)

data class FallMonthProjection(
    val label: String,
    val falls: Int,
)

// ─────────────────────────────────────────────────────────── episodes

data class EpisodesTabProjection(
    val residentId: String,
    val episodes: List<EpisodeListItemProjection>,
)

data class EpisodeListItemProjection(
    val id: String,
    val kind: String,
    val severity: String,
    val occurredAt: Instant,
    val injuryStatus: String?,
    val selfRecovery: Boolean?,
    val verdict: String?,
    val reviewNote: String?,
    val reviewedAt: Instant?,
)
