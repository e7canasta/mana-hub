package com.hub.insights.domain.find

import com.hub.insights.domain.derive.SleepDerived
import com.hub.insights.domain.derive.SleepInsights
import com.hub.insights.inbound.HubSleepDay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * SleepBriefing — las tarjetas KPI y la narrativa de sueño.
 *
 * Cada residente tiene una "foto" de su sueño: tiempo inquieto, salidas,
 * eficiencia. La narrativa describe en lenguaje natural cómo duerme
 * respecto de su propia línea base — no contra un promedio poblacional.
 */
class SleepBriefingSpec {

    @Nested
    inner class `Tarjetas KPI - lo que ve el director` {

        @Test
        fun `todas las metricas presentes generan 4 tarjetas`() {
            val days = sleepDays(calm = 251, restless = 97, awake = 45, exits = 3)
            val derived = SleepInsights.derive(days)
            val cards = SleepBriefing.cards(derived)
            assertThat(cards.map { it.code }).containsExactly(
                "RESTLESS", "BED_EXITS", "TIME_IN_BED", "EFFICIENCY",
            )
        }

        @Test
        fun `tarjeta de sueño inquieto muestra minutos y proporcion`() {
            val days = sleepDays(calm = 251, restless = 97, awake = 45, exits = 3)
            val derived = SleepInsights.derive(days)
            val restless = SleepBriefing.cards(derived).single { it.code == "RESTLESS" }
            assertThat(restless.value).isEqualTo("1h 37")
            assertThat(restless.detail).contains("28%")
        }

        @Test
        fun `tarjeta de salidas muestra promedio y maximo`() {
            val days = sleepDays(calm = 251, restless = 97, awake = 45, exits = 3)
            val derived = SleepInsights.derive(days)
            val exits = SleepBriefing.cards(derived).single { it.code == "BED_EXITS" }
            assertThat(exits.value).isEqualTo("3.0")
            assertThat(exits.detail).contains("máximo")
        }

        @Test
        fun `tarjeta de eficiencia muestra porcentaje`() {
            val days = sleepDays(calm = 251, restless = 97, awake = 45, exits = 3)
            val derived = SleepInsights.derive(days)
            val eff = SleepBriefing.cards(derived).single { it.code == "EFFICIENCY" }
            assertThat(eff.value).endsWith("%")
            assertThat(eff.detail).contains("dormido sobre tiempo en cama")
        }

        @Test
        fun `sin datos de sueño inquieto no genera tarjeta RESTLESS`() {
            val days = (1..14).map { HubSleepDay(day = "2026-08-${18 + it}", calmMinutes = 300, restlessMinutes = 0, measured = false) }
            val derived = SleepInsights.derive(days)
            val codes = SleepBriefing.cards(derived).map { it.code }
            assertThat(codes).doesNotContain("RESTLESS")
        }
    }

    @Nested
    inner class `Narrativa - la voz del sistema` {

        @Test
        fun `narrativa describe horas dormidas y rango habitual`() {
            val days = sleepDays(calm = 251, restless = 97, awake = 45, exits = 3)
            val derived = SleepInsights.derive(days)
            val text = SleepBriefing.narrative(derived, days)!!
            assertThat(text).startsWith("Durmió")
            assertThat(text).contains("por noche en promedio")
            assertThat(text).contains("dentro de su rango habitual")
        }

        @Test
        fun `narrativa incluye salidas de cama cuando hay`() {
            val days = sleepDays(calm = 251, restless = 97, awake = 45, exits = 3)
            val derived = SleepInsights.derive(days)
            val text = SleepBriefing.narrative(derived, days)!!
            assertThat(text).contains("salidas de cama por noche")
        }

        @Test
        fun `narrativa sin datos de sueño devuelve null`() {
            val derived = SleepDerived(
                avgCalmMinutes7d = null, deltaCalmMinutesWoW = null,
                avgRestlessMinutes7d = null, avgAsleepMinutes7d = null,
                restlessShare = null, avgBedExits = null, maxBedExits = null,
                avgTimeInBedMinutes = null, sleepEfficiency = null,
                habitualFrom = null, habitualTo = null,
            )
            assertThat(SleepBriefing.narrative(derived, emptyList())).isNull()
        }
    }

    @Nested
    inner class `Comparacion con semana anterior` {

        @Test
        fun `salidas en aumento se refleja en la narrativa`() {
            val days = sleepWeeks(prevExits = 2, lastExits = listOf(3, 3, 3, 3, 2, 2, 3))
            val derived = SleepInsights.derive(days)
            val text = SleepBriefing.narrative(derived, days)!!
            assertThat(text).contains("Las salidas vienen aumentando")
        }

        @Test
        fun `salidas estables no menciona aumento`() {
            val days = sleepWeeks(prevExits = 3, lastExits = listOf(3, 3, 3, 3, 3, 3, 3))
            val derived = SleepInsights.derive(days)
            val text = SleepBriefing.narrative(derived, days)!!
            assertThat(text).doesNotContain("aumentando")
        }
    }

    @Nested
    inner class `Clasificacion por rangos` {

        @Test
        fun `dentro del rango habitual`() {
            val days = sleepDays(calm = 251, restless = 97, awake = 45, exits = 3)
            val clause = SleepBriefing.rangeClause(348, days)
            assertThat(clause).contains("dentro de su rango habitual")
        }

        @Test
        fun `por debajo del rango habitual`() {
            val days = sleepDays(calm = 251, restless = 97, awake = 45, exits = 3)
            val clause = SleepBriefing.rangeClause(200, days)
            assertThat(clause).contains("por debajo de su rango habitual")
        }

        @Test
        fun `por encima del rango habitual`() {
            val days = sleepDays(calm = 251, restless = 97, awake = 45, exits = 3)
            val clause = SleepBriefing.rangeClause(500, days)
            assertThat(clause).contains("por encima de su rango habitual")
        }

        @Test
        fun `sin datos de sueño no genera comparacion`() {
            val clause = SleepBriefing.rangeClause(300, emptyList())
            assertThat(clause).isEmpty()
        }
    }

    @Nested
    inner class `Separacion en semanas` {

        @Test
        fun `14 dias se separan en dos semanas de 7`() {
            val days = (1..14).map { HubSleepDay(day = "2026-08-${18 + it}", calmMinutes = 300) }
            val (last7, prev7) = SleepBriefing.weeks(days)
            assertThat(last7).hasSize(7)
            assertThat(prev7).hasSize(7)
        }

        @Test
        fun `menos de 7 dias en la segunda semana`() {
            val days = (1..10).map { HubSleepDay(day = "2026-08-${18 + it}", calmMinutes = 300) }
            val (last7, prev7) = SleepBriefing.weeks(days)
            assertThat(last7).hasSize(7)
            assertThat(prev7).hasSize(3)
        }

        @Test
        fun `solo 7 dias - la semana anterior esta vacia`() {
            val days = (1..7).map { HubSleepDay(day = "2026-08-${18 + it}", calmMinutes = 300) }
            val (last7, prev7) = SleepBriefing.weeks(days)
            assertThat(last7).hasSize(7)
            assertThat(prev7).isEmpty()
        }

        @Test
        fun `dias sin medir se ignoran`() {
            val start = java.time.LocalDate.of(2026, 8, 1)
            val days = (0 until 14).map { n ->
                HubSleepDay(
                    day = start.plusDays(n.toLong()).toString(),
                    calmMinutes = 300,
                    measured = n > 3, // primeros 4 sin medir
                )
            }
            val (last7, prev7) = SleepBriefing.weeks(days)
            assertThat(last7).hasSize(7)
            assertThat(prev7).hasSize(3)
        }
    }

    @Nested
    inner class `Deteccion de tendencia ascendente` {

        @Test
        fun `salidas suben mas del 15 por ciento y al menos 3 decimas`() {
            val days = sleepWeeks(prevExits = 2, lastExits = listOf(3, 3, 3, 3, 3, 3, 3))
            assertThat(SleepBriefing.exitsRising(days)).isTrue()
        }

        @Test
        fun `salidas estables no son ascendentes`() {
            val days = sleepWeeks(prevExits = 3, lastExits = listOf(3, 3, 3, 3, 3, 3, 3))
            assertThat(SleepBriefing.exitsRising(days)).isFalse()
        }

        @Test
        fun `menos de 7 dias no detecta tendencia`() {
            val days = (1..5).map { HubSleepDay(day = "2026-08-${18 + it}", calmMinutes = 300, bedExitCount = 3) }
            assertThat(SleepBriefing.exitsRising(days)).isFalse()
        }
    }

    private fun sleepDays(
        calm: Int = 251,
        restless: Int = 97,
        awake: Int = 45,
        exits: Int = 3,
    ): List<HubSleepDay> = (1..14).map { n ->
        HubSleepDay(
            day = "2026-08-${18 + n}",
            calmMinutes = calm,
            restlessMinutes = restless,
            awakeMinutes = awake,
            outOfBedMinutes = 20,
            bedExitCount = exits,
        )
    }

    private fun sleepWeeks(
        prevExits: Int,
        lastExits: List<Int>,
        calm: Int = 251,
        restless: Int = 97,
        awake: Int = 45,
    ): List<HubSleepDay> {
        val start = java.time.LocalDate.of(2026, 8, 18)
        val padded = if (lastExits.size >= 7) lastExits.take(7) else lastExits + List(7 - lastExits.size) { lastExits.last() }
        return (0 until 14).map { n ->
            val exits = if (n < 7) prevExits else padded[n - 7]
            HubSleepDay(
                day = start.plusDays(n.toLong()).toString(),
                calmMinutes = calm,
                restlessMinutes = restless,
                awakeMinutes = awake,
                outOfBedMinutes = 20,
                bedExitCount = exits,
            )
        }
    }
}
