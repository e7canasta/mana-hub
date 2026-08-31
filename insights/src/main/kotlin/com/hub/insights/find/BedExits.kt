package com.hub.insights.find

import com.hub.insights.rollup.ScenePoint
import com.hub.insights.rollup.StaffVisit
import com.hub.insights.rollup.inBed
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object BedExits {
    val DAWN_FROM: LocalTime = LocalTime.of(5, 0)
    val DAWN_TO: LocalTime = LocalTime.of(6, 5)
    val STAFF_AFTER: Duration = Duration.ofMinutes(20)

    fun fromPoints(points: List<ScenePoint>): List<Instant> =
        points.filter { it.from?.inBed == true && !it.to.inBed }.map { it.at }

    fun inLocalDays(at: Instant, from: LocalDate, to: LocalDate, zone: ZoneId): Boolean {
        val day = at.atZone(zone).toLocalDate()
        return !day.isBefore(from) && !day.isAfter(to)
    }

    fun isDawn(at: Instant, zone: ZoneId): Boolean {
        val time = at.atZone(zone).toLocalTime()
        return !time.isBefore(DAWN_FROM) && !time.isAfter(DAWN_TO)
    }

    fun staffAfter(exits: List<Instant>, visits: List<StaffVisit>): Int =
        exits.count { exit ->
            visits.any { visit ->
                !visit.enteredAt.isBefore(exit) &&
                    Duration.between(exit, visit.enteredAt) <= STAFF_AFTER
            }
        }
}
