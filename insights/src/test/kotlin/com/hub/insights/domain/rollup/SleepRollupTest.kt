package com.hub.insights.domain.rollup

import com.hub.insights.inbound.HubSceneEvent
import com.manahive.contracts.scene.PersonState
import com.manahive.contracts.scene.StateKind
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SleepRollupTest {

    private val zone = ZoneId.of("America/Argentina/Buenos_Aires")

    @Test
    fun `E1 vuelve solo — Lying, SittingInBed 17m, Lying`() {
        val events = e1VuelveSolo()
        val points = SceneTimeline.points(events)
        val start = Instant.parse("2026-09-03T22:00:00Z")
        val end = Instant.parse("2026-09-04T00:32:00Z")
        val dwells = SceneTimeline.dwells(points, start, end)
        val sleep = SleepRollup.compute(dwells, LocalDate.of(2026, 9, 3), zone, sleepOnsetMinutes = 10)

        // 75m primer Lying: 10 latencia + 65 profundo; 17m SittingInBed despierto; 60m Lying todo profundo
        assertThat(sleep.calmMinutes).isEqualTo(125)
        assertThat(sleep.restlessMinutes).isEqualTo(10)
        assertThat(sleep.awakeMinutes).isEqualTo(17)
        assertThat(sleep.wakeCount).isEqualTo(1)
        assertThat(sleep.bedExitCount).isEqualTo(0)
        assertThat(sleep.outOfBedMinutes).isEqualTo(0)
    }

    @Test
    fun `ComeBackExceeded no cambia el FSM — no abre dwell`() {
        val events = e1VuelveSolo()
        val points = SceneTimeline.points(events)
        assertThat(points).hasSize(3)
        assertThat(points.map { it.to }).containsExactly(
            StateKind.LYING,
            StateKind.SITTING_IN_BED,
            StateKind.LYING,
        )
    }

    @Test
    fun `salida Lying a Standing cuenta bed exit`() {
        val events = listOf(
            transition("2026-09-03T22:00:00Z", "Unknown", PersonState.Lying),
            transition("2026-09-03T23:00:00Z", PersonState.Lying, PersonState.Standing),
            transition("2026-09-03T23:10:00Z", PersonState.Standing, PersonState.Lying),
        )
        val dwells = SceneTimeline.dwells(
            SceneTimeline.points(events),
            Instant.parse("2026-09-03T22:00:00Z"),
            Instant.parse("2026-09-03T23:40:00Z"),
        )
        val sleep = SleepRollup.compute(dwells, LocalDate.of(2026, 9, 3), zone, sleepOnsetMinutes = 10)
        assertThat(sleep.bedExitCount).isEqualTo(1)
        assertThat(sleep.outOfBedMinutes).isEqualTo(10)
        // 60m primer Lying (10 latencia + 50 profundo) + 30m Lying (todo profundo) = 80
        assertThat(sleep.calmMinutes).isEqualTo(80)
    }

    @Test
    fun `InBathroom cuenta visita no restless`() {
        val events = listOf(
            transition("2026-09-03T22:00:00Z", "Unknown", PersonState.Lying),
            transition("2026-09-03T23:00:00Z", PersonState.Lying, PersonState.InBathroom),
            transition("2026-09-03T23:08:00Z", PersonState.InBathroom, PersonState.Lying),
        )
        val dwells = SceneTimeline.dwells(
            SceneTimeline.points(events),
            Instant.parse("2026-09-03T22:00:00Z"),
            Instant.parse("2026-09-03T23:20:00Z"),
        )
        val bath = BathroomRollup.compute(dwells, LocalDate.of(2026, 9, 3), zone)
        assertThat(bath.visitCount).isEqualTo(1)
        assertThat(bath.totalMinutes).isEqualTo(8)
        val sleep = SleepRollup.compute(dwells, LocalDate.of(2026, 9, 3), zone, sleepOnsetMinutes = 10)
        assertThat(sleep.outOfBedMinutes).isEqualTo(8)
    }

    @Test
    fun `InHallway cuenta como andar no como en cama`() {
        val events = listOf(
            transition("2026-09-03T12:00:00Z", "Unknown", PersonState.InRoom),
            transition("2026-09-03T12:10:00Z", PersonState.InRoom, PersonState.InHallway),
            transition("2026-09-03T12:25:00Z", PersonState.InHallway, PersonState.InRoom),
        )
        val dwells = SceneTimeline.dwells(
            SceneTimeline.points(events),
            Instant.parse("2026-09-03T12:00:00Z"),
            Instant.parse("2026-09-03T12:30:00Z"),
        )
        val mobility = MobilityRollup.compute(dwells, LocalDate.of(2026, 9, 3))
        assertThat(mobility.walkingMinutes).isEqualTo(15)
        assertThat(mobility.outOfBedMinutes).isEqualTo(30)
    }

    @Test
    fun `StaffPresence y StaffLeft arman visita de cuidado`() {
        val events = listOf(
            HubSceneEvent(type = SceneEventTypes.STAFF_PRESENCE, at = Instant.parse("2026-09-03T14:00:00Z")),
            HubSceneEvent(type = SceneEventTypes.STAFF_LEFT, at = Instant.parse("2026-09-03T14:12:00Z")),
        )
        val visits = SceneTimeline.staffVisits(
            events,
            Instant.parse("2026-09-03T00:00:00Z"),
            Instant.parse("2026-09-04T00:00:00Z"),
        )
        val care = CareRollup.compute(visits, LocalDate.of(2026, 9, 3))
        assertThat(care.visitCount).isEqualTo(1)
        assertThat(care.totalMinutes).isEqualTo(12)
        assertThat(care.proactiveMinutes).isEqualTo(0)
    }

    @Test
    fun `NightOpened abre el FSM con initialState no con to`() {
        val events = listOf(
            HubSceneEvent(
                type = SceneEventTypes.NIGHT_OPENED,
                initialState = PersonState.Lying::class.simpleName,
                at = Instant.parse("2026-09-03T22:00:00Z"),
            ),
            transition("2026-09-03T22:40:00Z", PersonState.Lying, PersonState.SittingInBed),
        )
        val points = SceneTimeline.points(events)
        assertThat(points).hasSize(2)
        assertThat(points[0].to).isEqualTo(StateKind.LYING)
        val dwells = SceneTimeline.dwells(
            points,
            Instant.parse("2026-09-03T22:00:00Z"),
            Instant.parse("2026-09-03T22:45:00Z"),
        )
        val sleep = SleepRollup.compute(dwells, LocalDate.of(2026, 9, 3), zone, sleepOnsetMinutes = 10)
        assertThat(sleep.calmMinutes).isEqualTo(30)
        assertThat(sleep.restlessMinutes).isEqualTo(10)
        assertThat(sleep.awakeMinutes).isEqualTo(5)
    }

    @Test
    fun `DwellExceeded y SceneStateChanged no abren dwell de persona`() {
        val events = listOf(
            transition("2026-09-03T22:00:00Z", "Unknown", PersonState.Lying),
            HubSceneEvent(
                type = SceneEventTypes.DWELL_EXCEEDED,
                at = Instant.parse("2026-09-03T22:20:00Z"),
            ),
            HubSceneEvent(
                type = SceneEventTypes.SCENE_STATE_CHANGED,
                from = "NotPresent",
                to = "Present",
                at = Instant.parse("2026-09-03T22:21:00Z"),
            ),
            transition("2026-09-03T22:30:00Z", PersonState.Lying, PersonState.Standing),
        )
        val points = SceneTimeline.points(events)
        assertThat(points.map { it.to }).containsExactly(StateKind.LYING, StateKind.STANDING)
    }

    @Test
    fun `la latencia de sueño no se reaplica en cada Lying`() {
        val events = listOf(
            transition("2026-09-03T22:00:00Z", "Unknown", PersonState.Lying),
            transition("2026-09-03T22:20:00Z", PersonState.Lying, PersonState.SittingInBed),
            transition("2026-09-03T22:25:00Z", PersonState.SittingInBed, PersonState.Lying),
        )
        val dwells = SceneTimeline.dwells(
            SceneTimeline.points(events),
            Instant.parse("2026-09-03T22:00:00Z"),
            Instant.parse("2026-09-03T22:55:00Z"),
        )
        val sleep = SleepRollup.compute(dwells, LocalDate.of(2026, 9, 3), zone, sleepOnsetMinutes = 10)
        // 20m primer Lying: 10+10; 5m sitting; 30m segundo Lying: 30 calm
        assertThat(sleep.restlessMinutes).isEqualTo(10)
        assertThat(sleep.calmMinutes).isEqualTo(40)
        assertThat(sleep.awakeMinutes).isEqualTo(5)
    }

    @Test
    fun `visita de baño de 50s no se pierde`() {
        val events = listOf(
            transition("2026-09-03T23:00:00Z", PersonState.Lying, PersonState.InBathroom),
            transition("2026-09-03T23:00:50Z", PersonState.InBathroom, PersonState.Lying),
        )
        val dwells = SceneTimeline.dwells(
            SceneTimeline.points(events),
            Instant.parse("2026-09-03T23:00:00Z"),
            Instant.parse("2026-09-03T23:01:00Z"),
        )
        val bath = BathroomRollup.compute(dwells, LocalDate.of(2026, 9, 3), zone)
        assertThat(bath.visitCount).isEqualTo(1)
        assertThat(bath.totalMinutes).isEqualTo(1)
    }

    private fun e1VuelveSolo() = listOf(
        transition("2026-09-03T22:00:00Z", "Unknown", PersonState.Lying),
        transition("2026-09-03T23:15:00Z", PersonState.Lying, PersonState.SittingInBed),
        HubSceneEvent(
            type = SceneEventTypes.COME_BACK_EXCEEDED,
            at = Instant.parse("2026-09-03T23:32:00Z"),
        ),
        transition("2026-09-03T23:32:00Z", PersonState.SittingInBed, PersonState.Lying),
    )

    private fun transition(at: String, from: PersonState, to: PersonState) =
        transition(at, from::class.simpleName!!, to)

    private fun transition(at: String, from: String, to: PersonState) = HubSceneEvent(
        type = SceneEventTypes.TRANSITION,
        from = from,
        to = to::class.simpleName,
        at = Instant.parse(at),
    )
}
