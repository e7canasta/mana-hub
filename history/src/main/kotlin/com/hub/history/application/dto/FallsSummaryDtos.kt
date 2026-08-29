package com.hub.history.application.dto

data class FallsMonthSummary(
    val label: String,
    val falls: Int
)

data class FallsSummaryResponse(
    val residentId: String,
    val streakDays: Int,
    val previousStreakDays: Int,
    val fallsLast12Months: Int,
    val lastFallAt: String?,
    val lastFallInjury: String?,
    val months: List<FallsMonthSummary>
)
