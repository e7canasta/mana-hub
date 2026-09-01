package com.hub.insights.domain.find

import com.hub.insights.domain.rollup.ScenePoint
import com.hub.insights.domain.rollup.SceneEventTypes
import com.hub.insights.domain.rollup.StaffVisit
import com.hub.insights.domain.rollup.inBed
import com.manahive.contracts.scene.StateKind
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * BedExits — la lógica de detección de salidas de cama.
 *
 * Un "exit" es cuando el residente pasa de un estado `inBed` a uno que no lo es.
 * La ventana de alba (05:00–06:05) es crítica: las salidas en esa franja
 * se agrupan en un "cluster de alba" que dispara alertas de política.
 */
class BedExitsSpec {

    private val zone = ZoneId.of("America/Argentina/Buenos_Aires")

    @Nested
    inner class `Detectar salidas de cama` {

        @Test
        fun `una transicion de Lying a Standing es una salida`() {
            val point = scenePoint(at = "2026-08-30T08:15:00Z", from = StateKind.LYING, to = StateKind.STANDING)
            val exits = BedExits.fromPoints(listOf(point))
            assertThat(exits).containsExactly(Instant.parse("2026-08-30T08:15:00Z"))
        }

        @Test
        fun `SittingInBed a Standing tambien cuenta`() {
            val point = scenePoint(at = "2026-08-30T09:00:00Z", from = StateKind.SITTING_IN_BED, to = StateKind.STANDING)
            val exits = BedExits.fromPoints(listOf(point))
            assertThat(exits).hasSize(1)
        }

        @Test
        fun `BedEdge a Standing cuenta como salida`() {
            val point = scenePoint(at = "2026-08-30T07:30:00Z", from = StateKind.BED_EDGE, to = StateKind.STANDING)
            val exits = BedExits.fromPoints(listOf(point))
            assertThat(exits).hasSize(1)
        }

        @Test
        fun `Standing a Lying no es salida — es volver a la cama`() {
            val point = scenePoint(at = "2026-08-30T08:15:00Z", from = StateKind.STANDING, to = StateKind.LYING)
            val exits = BedExits.fromPoints(listOf(point))
            assertThat(exits).isEmpty()
        }

        @Test
        fun `Lying a SittingInBed no es salida — sigue en cama`() {
            val point = scenePoint(at = "2026-08-30T08:15:00Z", from = StateKind.LYING, to = StateKind.SITTING_IN_BED)
            val exits = BedExits.fromPoints(listOf(point))
            assertThat(exits).isEmpty()
        }

        @Test
        fun `multiples transiciones se cuentan todas`() {
            val points = listOf(
                scenePoint(at = "2026-08-30T05:10:00Z", from = StateKind.LYING, to = StateKind.STANDING),
                scenePoint(at = "2026-08-30T05:30:00Z", from = StateKind.LYING, to = StateKind.STANDING),
                scenePoint(at = "2026-08-30T06:00:00Z", from = StateKind.LYING, to = StateKind.STANDING),
            )
            assertThat(BedExits.fromPoints(points)).hasSize(3)
        }
    }

    @Nested
    inner class `Ventana de alba 05 00 a 06 05` {

        @Test
        fun `una salida a las 05 15 es alba`() {
            val at = Instant.parse("2026-08-30T08:15:00Z") // 05:15 ART
            assertThat(BedExits.isDawn(at, zone)).isTrue()
        }

        @Test
        fun `una salida a las 06 00 es alba`() {
            val at = Instant.parse("2026-08-30T09:00:00Z") // 06:00 ART
            assertThat(BedExits.isDawn(at, zone)).isTrue()
        }

        @Test
        fun `una salida a las 04 59 NO es alba`() {
            val at = Instant.parse("2026-08-30T07:59:00Z") // 04:59 ART
            assertThat(BedExits.isDawn(at, zone)).isFalse()
        }

        @Test
        fun `una salida a las 06 06 NO es alba`() {
            val at = Instant.parse("2026-08-30T09:06:00Z") // 06:06 ART
            assertThat(BedExits.isDawn(at, zone)).isFalse()
        }

        @Test
        fun `una salida a las 14 00 NO es alba`() {
            val at = Instant.parse("2026-08-30T17:00:00Z") // 14:00 ART
            assertThat(BedExits.isDawn(at, zone)).isFalse()
        }
    }

    @Nested
    inner class `Filtrar salidas por rango de dias` {

        @Test
        fun `una salida dentro del rango pasa el filtro`() {
            val at = Instant.parse("2026-08-28T10:00:00Z")
            assertThat(BedExits.inLocalDays(at, LocalDate.of(2026, 8, 25), LocalDate.of(2026, 8, 30), zone)).isTrue()
        }

        @Test
        fun `una salida exactamente en el dia de inicio pasa`() {
            val at = Instant.parse("2026-08-25T03:00:00Z") // 00:00 ART
            assertThat(BedExits.inLocalDays(at, LocalDate.of(2026, 8, 25), LocalDate.of(2026, 8, 30), zone)).isTrue()
        }

        @Test
        fun `una salida exactamente en el dia de fin pasa`() {
            val at = Instant.parse("2026-08-31T02:59:00Z") // 23:59 ART del 30
            assertThat(BedExits.inLocalDays(at, LocalDate.of(2026, 8, 25), LocalDate.of(2026, 8, 30), zone)).isTrue()
        }

        @Test
        fun `una salida antes del rango no pasa`() {
            val at = Instant.parse("2026-08-24T23:00:00Z") // 20:00 ART del 24
            assertThat(BedExits.inLocalDays(at, LocalDate.of(2026, 8, 25), LocalDate.of(2026, 8, 30), zone)).isFalse()
        }

        @Test
        fun `una salida despues del rango no pasa`() {
            val at = Instant.parse("2026-08-31T03:00:00Z") // 00:00 ART del 31
            assertThat(BedExits.inLocalDays(at, LocalDate.of(2026, 8, 25), LocalDate.of(2026, 8, 30), zone)).isFalse()
        }
    }

    @Nested
    inner class `Staff despues de una salida` {

        @Test
        fun `enfermera que llega 10 minutos despues cuenta`() {
            val exit = Instant.parse("2026-08-30T08:00:00Z")
            val visit = StaffVisit(
                enteredAt = Instant.parse("2026-08-30T08:10:00Z"),
                leftAt = Instant.parse("2026-08-30T08:20:00Z"),
            )
            assertThat(BedExits.staffAfter(listOf(exit), listOf(visit))).isEqualTo(1)
        }

        @Test
        fun `enfermera que llega 21 minutos despues NO cuenta`() {
            val exit = Instant.parse("2026-08-30T08:00:00Z")
            val visit = StaffVisit(
                enteredAt = Instant.parse("2026-08-30T08:21:00Z"),
                leftAt = Instant.parse("2026-08-30T08:30:00Z"),
            )
            assertThat(BedExits.staffAfter(listOf(exit), listOf(visit))).isEqualTo(0)
        }

        @Test
        fun `enfermera que llega justo a los 20 minutos cuenta`() {
            val exit = Instant.parse("2026-08-30T08:00:00Z")
            val visit = StaffVisit(
                enteredAt = Instant.parse("2026-08-30T08:20:00Z"),
                leftAt = Instant.parse("2026-08-30T08:30:00Z"),
            )
            assertThat(BedExits.staffAfter(listOf(exit), listOf(visit))).isEqualTo(1)
        }

        @Test
        fun `sin visitas de staff el conteo es cero`() {
            val exit = Instant.parse("2026-08-30T08:00:00Z")
            assertThat(BedExits.staffAfter(listOf(exit), emptyList())).isEqualTo(0)
        }

        @Test
        fun `varias salidas con staff variable`() {
            val exit1 = Instant.parse("2026-08-30T05:10:00Z")
            val exit2 = Instant.parse("2026-08-30T05:30:00Z")
            val exit3 = Instant.parse("2026-08-30T06:00:00Z")
            val visits = listOf(
                StaffVisit(Instant.parse("2026-08-30T05:15:00Z"), Instant.parse("2026-08-30T05:25:00Z")),
                StaffVisit(Instant.parse("2026-08-30T06:30:00Z"), Instant.parse("2026-08-30T06:40:00Z")),
            )
            // exit1 tiene staff a los 5min, exit2 no tiene, exit3 tiene staff a los 30min (>20min)
            assertThat(BedExits.staffAfter(listOf(exit1, exit2, exit3), visits)).isEqualTo(1)
        }
    }

    private fun scenePoint(at: String, from: StateKind?, to: StateKind) = ScenePoint(
        at = Instant.parse(at),
        from = from,
        to = to,
        type = SceneEventTypes.TRANSITION,
    )
}
