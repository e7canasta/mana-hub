package com.hub.insights.rollup

import com.hub.insights.inbound.HubSceneEvent
import com.manahive.contracts.scene.StateKind
import java.time.Duration
import java.time.Instant

data class ScenePoint(
    val at: Instant,
    val from: StateKind?,
    val to: StateKind,
    val type: String,
)

data class Dwell(
    val start: Instant,
    val end: Instant,
    val kind: StateKind,
    val fromKind: StateKind?,
) {
    val minutes: Int
        get() = Duration.between(start, end).seconds.coerceAtLeast(0).toInt() / 60
}

object SceneTimeline {

    /**
     * Solo hechos que cambian el FSM de persona:
     * [SceneEvent.TransitionDetected], [SceneEvent.NightOpened].
     * ComeBackExceeded / DwellExceeded se descartan (mismo estado).
     */
    fun points(events: List<HubSceneEvent>): List<ScenePoint> =
        events.mapNotNull { ev ->
            if (!SceneEventTypes.changesPersonState(ev.typeName())) return@mapNotNull null
            val at = ev.occurredAtInstant() ?: return@mapNotNull null
            val to = PersonStateCodec.parse(ev.toName()) ?: return@mapNotNull null
            ScenePoint(
                at = at,
                from = PersonStateCodec.parse(ev.fromName()),
                to = to,
                type = ev.typeName() ?: SceneEventTypes.TRANSITION,
            )
        }.sortedBy { it.at }

    fun dwells(points: List<ScenePoint>, windowStart: Instant, windowEnd: Instant): List<Dwell> {
        if (!windowEnd.isAfter(windowStart)) return emptyList()
        val inWindow = points.filter { it.at < windowEnd }
        if (inWindow.isEmpty()) return emptyList()

        val result = mutableListOf<Dwell>()
        val first = inWindow.first()
        if (first.at.isAfter(windowStart) && first.from != null) {
            result += Dwell(windowStart, min(first.at, windowEnd), first.from, null)
        }

        for (i in inWindow.indices) {
            val cur = inWindow[i]
            val start = max(cur.at, windowStart)
            val end = if (i + 1 < inWindow.size) min(inWindow[i + 1].at, windowEnd) else windowEnd
            if (!end.isAfter(start)) continue
            result += Dwell(start, end, cur.to, cur.from)
        }
        return result
    }

    /**
     * Visitas de staff: [SceneEvent.StaffPresenceDetected] → [SceneEvent.StaffLeftDetected].
     */
    fun staffVisits(events: List<HubSceneEvent>, windowStart: Instant, windowEnd: Instant): List<StaffVisit> {
        if (!windowEnd.isAfter(windowStart)) return emptyList()
        val stamps = events.mapNotNull { ev ->
            val at = ev.occurredAtInstant() ?: return@mapNotNull null
            when (ev.typeName()) {
                SceneEventTypes.STAFF_PRESENCE -> at to true
                SceneEventTypes.STAFF_LEFT -> at to false
                else -> null
            }
        }.filter { it.first < windowEnd }.sortedBy { it.first }

        val visits = mutableListOf<StaffVisit>()
        var entered: Instant? = null
        for ((at, isEnter) in stamps) {
            if (isEnter) {
                entered = max(at, windowStart)
            } else if (entered != null) {
                val end = min(at, windowEnd)
                if (end.isAfter(entered)) visits += StaffVisit(entered, end)
                entered = null
            }
        }
        if (entered != null && windowEnd.isAfter(entered)) {
            visits += StaffVisit(entered, windowEnd)
        }
        return visits
    }

    private fun min(a: Instant, b: Instant) = if (a.isBefore(b)) a else b
    private fun max(a: Instant, b: Instant) = if (a.isAfter(b)) a else b
}

data class StaffVisit(
    val enteredAt: Instant,
    val leftAt: Instant,
) {
    val minutes: Int
        get() = Duration.between(enteredAt, leftAt).seconds.coerceAtLeast(0).toInt() / 60
}
