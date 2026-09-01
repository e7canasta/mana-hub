package com.hub.insights.domain.find

import com.hub.insights.domain.rollup.ScenePoint
import com.hub.insights.domain.rollup.StaffVisit
import com.hub.insights.domain.rollup.inBed
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

    fun isDawn(at: Instant, zone: ZoneId, policy: SleepPolicy): Boolean {
        val from = LocalTime.parse(policy.dawnFrom)
        val to = LocalTime.parse(policy.dawnTo)
        val time = at.atZone(zone).toLocalTime()
        return !time.isBefore(from) && !time.isAfter(to)
    }

    fun staffAfter(exits: List<Instant>, visits: List<StaffVisit>): Int =
        exits.count { exit ->
            visits.any { visit ->
                !visit.enteredAt.isBefore(exit) &&
                    Duration.between(exit, visit.enteredAt) <= STAFF_AFTER
            }
        }
}
