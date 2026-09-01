package com.hub.insights.domain.recommend

import com.hub.insights.domain.derive.Baseline
import com.hub.insights.domain.derive.SleepDerived
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * WellbeingRecommendations — las recomendaciones clínicas.
 *
 * Cada recomendación es una "opinión del sistema" basada en la línea base
 * del residente. No son alertas fijas: se comparan contra cómo duerme
 * *esta persona* habitualmente.
 */
class WellbeingRecommendationsExtendedSpec {

    @Nested
    inner class `Recomendaciones de sueño` {

        @Test
        fun `linea base en formacion solo retorna BASELINE_FORMING`() {
            val baseline = forming(1)
            val recs = WellbeingRecommendations.forSleep(baseline, derived(restlessShare = 0.9))
            assertThat(recs).hasSize(1)
            assertThat(recs.first().code).isEqualTo("BASELINE_FORMING")
            assertThat(recs.first().severity).isEqualTo("info")
        }

        @Test
        fun `dia singular en el texto de formacion`() {
            val recs = WellbeingRecommendations.forSleep(forming(1), derived())
            assertThat(recs.first().text).contains("1 día")
            assertThat(recs.first().text).doesNotContain("1 días")
        }

        @Test
        fun `dia plural en el texto de formacion`() {
            val recs = WellbeingRecommendations.forSleep(forming(3), derived())
            assertThat(recs.first().text).contains("3 días")
        }

        @Test
        fun `restless share null no genera recomendacion de rango`() {
            val recs = WellbeingRecommendations.forSleep(ready, derived(restlessShare = null))
            assertThat(recs).isEmpty()
        }

        @Test
        fun `restless share dentro del rango genera SLEEP_IN_RANGE`() {
            val recs = WellbeingRecommendations.forSleep(ready, derived(restlessShare = 0.15))
            assertThat(recs).hasSize(1)
            assertThat(recs.first().code).isEqualTo("SLEEP_IN_RANGE")
        }

        @Test
        fun `restless share exactamente 20 por ciento sigue en rango`() {
            val recs = WellbeingRecommendations.forSleep(ready, derived(restlessShare = 0.20))
            assertThat(recs.first().code).isEqualTo("SLEEP_IN_RANGE")
        }

        @Test
        fun `restless share entre 20 y 35 genera SLEEP_RESTLESS`() {
            val recs = WellbeingRecommendations.forSleep(ready, derived(restlessShare = 0.30))
            assertThat(recs).hasSize(1)
            assertThat(recs.first().code).isEqualTo("SLEEP_RESTLESS")
            assertThat(recs.first().severity).isEqualTo("warning")
        }

        @Test
        fun `restless share exactamente 35 por ciento sigue en RESTLESS`() {
            val recs = WellbeingRecommendations.forSleep(ready, derived(restlessShare = 0.35))
            assertThat(recs.first().code).isEqualTo("SLEEP_RESTLESS")
        }

        @Test
        fun `restless share mayor a 35 genera SLEEP_FRAGMENTED`() {
            val recs = WellbeingRecommendations.forSleep(ready, derived(restlessShare = 0.45))
            assertThat(recs).hasSize(1)
            assertThat(recs.first().code).isEqualTo("SLEEP_FRAGMENTED")
            assertThat(recs.first().severity).isEqualTo("warning")
        }

        @Test
        fun `delta negativo mayor a 45 minutos agrega SLEEP_DROP_WOW`() {
            val recs = WellbeingRecommendations.forSleep(
                ready,
                derived(restlessShare = 0.15, deltaCalmMinutesWoW = -60),
            )
            assertThat(recs).hasSize(2)
            assertThat(recs.map { it.code }).contains("SLEEP_DROP_WOW")
        }

        @Test
        fun `delta negativo exactamente -45 agrega SLEEP_DROP_WOW`() {
            val recs = WellbeingRecommendations.forSleep(
                ready,
                derived(restlessShare = 0.15, deltaCalmMinutesWoW = -45),
            )
            assertThat(recs.map { it.code }).contains("SLEEP_DROP_WOW")
        }

        @Test
        fun `delta negativo de -44 no agrega SLEEP_DROP_WOW`() {
            val recs = WellbeingRecommendations.forSleep(
                ready,
                derived(restlessShare = 0.15, deltaCalmMinutesWoW = -44),
            )
            assertThat(recs.map { it.code }).doesNotContain("SLEEP_DROP_WOW")
        }

        @Test
        fun `delta positivo no agrega SLEEP_DROP_WOW`() {
            val recs = WellbeingRecommendations.forSleep(
                ready,
                derived(restlessShare = 0.15, deltaCalmMinutesWoW = 30),
            )
            assertThat(recs.map { it.code }).doesNotContain("SLEEP_DROP_WOW")
        }

        @Test
        fun `delta null no agrega SLEEP_DROP_WOW`() {
            val recs = WellbeingRecommendations.forSleep(
                ready,
                derived(restlessShare = 0.15, deltaCalmMinutesWoW = null),
            )
            assertThat(recs.map { it.code }).doesNotContain("SLEEP_DROP_WOW")
        }

        @Test
        fun `SLEEP_FRAGMENTED y SLEEP_DROP_WOW pueden coexistir`() {
            val recs = WellbeingRecommendations.forSleep(
                ready,
                derived(restlessShare = 0.50, deltaCalmMinutesWoW = -60),
            )
            assertThat(recs.map { it.code }).containsExactlyInAnyOrder("SLEEP_FRAGMENTED", "SLEEP_DROP_WOW")
        }
    }

    @Nested
    inner class `Recomendaciones de cuidado` {

        @Test
        fun `linea base en formacion retorna CARE_BASELINE_FORMING`() {
            val recs = WellbeingRecommendations.forCare(forming(1), avgMinutes = null, totalMinutes = 0)
            assertThat(recs).hasSize(1)
            assertThat(recs.first().code).isEqualTo("CARE_BASELINE_FORMING")
        }

        @Test
        fun `sin rollup de cuidado retorna CARE_NOT_MEASURED`() {
            val recs = WellbeingRecommendations.forCare(ready, avgMinutes = null, totalMinutes = 0)
            assertThat(recs).hasSize(1)
            assertThat(recs.first().code).isEqualTo("CARE_NOT_MEASURED")
        }

        @Test
        fun `totalMinutes cero retorna CARE_NONE`() {
            val recs = WellbeingRecommendations.forCare(ready, avgMinutes = 0.0, totalMinutes = 0)
            assertThat(recs).hasSize(1)
            assertThat(recs.first().code).isEqualTo("CARE_NONE")
        }

        @Test
        fun `avgMinutes cero y totalMinutes cero retorna CARE_NONE`() {
            val recs = WellbeingRecommendations.forCare(ready, avgMinutes = 0.0, totalMinutes = 0)
            assertThat(recs.first().code).isEqualTo("CARE_NONE")
        }

        @Test
        fun `con datos de cuidado retorna lista vacia`() {
            val recs = WellbeingRecommendations.forCare(ready, avgMinutes = 25.0, totalMinutes = 350)
            assertThat(recs).isEmpty()
        }

        @Test
        fun `texto de CARE_BASELINE_FORMING menciona que no es caida`() {
            val recs = WellbeingRecommendations.forCare(forming(1), avgMinutes = null, totalMinutes = 0)
            assertThat(recs.first().text).contains("no es una caída de actividad")
        }

        @Test
        fun `texto de CARE_NOT_MEASURED menciona que no es cero`() {
            val recs = WellbeingRecommendations.forCare(ready, avgMinutes = null, totalMinutes = 0)
            assertThat(recs.first().text).contains("No es cero de visitas")
        }

        @Test
        fun `texto de CARE_NONE menciona que es cero medido`() {
            val recs = WellbeingRecommendations.forCare(ready, avgMinutes = 0.0, totalMinutes = 0)
            assertThat(recs.first().text).contains("Cero medido")
        }
    }

    @Nested
    inner class `Recomendaciones de episodio resuelto` {

        @Test
        fun `auto-recuperacion retorna EPISODE_SELF_RECOVERY`() {
            val recs = WellbeingRecommendations.forEpisodeResolved(selfRecovery = true, durationMinutes = 17)
            assertThat(recs).hasSize(1)
            assertThat(recs.first().code).isEqualTo("EPISODE_SELF_RECOVERY")
            assertThat(recs.first().severity).isEqualTo("info")
        }

        @Test
        fun `auto-recuperacion con duracion menciona el tiempo`() {
            val recs = WellbeingRecommendations.forEpisodeResolved(selfRecovery = true, durationMinutes = 17)
            assertThat(recs.first().text).contains("17m")
        }

        @Test
        fun `auto-recuperacion sin duracion no menciona tiempo`() {
            val recs = WellbeingRecommendations.forEpisodeResolved(selfRecovery = true, durationMinutes = null)
            assertThat(recs.first().text).doesNotContain("min")
        }

        @Test
        fun `intervencion de staff retorna EPISODE_STAFF_CLOSED`() {
            val recs = WellbeingRecommendations.forEpisodeResolved(selfRecovery = false, durationMinutes = 25)
            assertThat(recs).hasSize(1)
            assertThat(recs.first().code).isEqualTo("EPISODE_STAFF_CLOSED")
        }

        @Test
        fun `EPISODE_STAFF_CLOSED menciona cuidado reactivo`() {
            val recs = WellbeingRecommendations.forEpisodeResolved(selfRecovery = false, durationMinutes = null)
            assertThat(recs.first().text).contains("cuidado reactivo")
        }

        @Test
        fun `EPISODE_STAFF_CLOSED no es una ronda`() {
            val recs = WellbeingRecommendations.forEpisodeResolved(selfRecovery = false, durationMinutes = null)
            assertThat(recs.first().text).contains("no como ronda")
        }
    }

    @Nested
    inner class `formatMinutes — formateo con signo` {

        @Test
        fun `minutos negativos llevan signo menos`() {
            assertThat(formatMinutes(-60)).isEqualTo("−1h 00")
        }

        @Test
        fun `minutos positivos llevan signo mas`() {
            assertThat(formatMinutes(45)).isEqualTo("+45m")
        }

        @Test
        fun `cero no lleva signo`() {
            assertThat(formatMinutes(0)).isEqualTo("0m")
        }

        @Test
        fun `horas y minutos`() {
            assertThat(formatMinutes(90)).isEqualTo("+1h 30")
        }
    }

    private fun forming(days: Int) = Baseline(
        admissionDate = LocalDate.of(2026, 8, 30),
        observedFrom = LocalDate.of(2026, 8, 30),
        observedDays = days,
        ready = false,
    )

    private val ready = Baseline(
        admissionDate = LocalDate.of(2024, 1, 15),
        observedFrom = LocalDate.of(2026, 8, 18),
        observedDays = 30,
        ready = true,
    )

    private fun derived(
        restlessShare: Double? = 0.15,
        deltaCalmMinutesWoW: Int? = null,
    ) = SleepDerived(
        avgCalmMinutes7d = 251,
        deltaCalmMinutesWoW = deltaCalmMinutesWoW,
        avgRestlessMinutes7d = 97,
        avgAsleepMinutes7d = 348,
        restlessShare = restlessShare,
        avgBedExits = 2.5,
        maxBedExits = 4,
        avgTimeInBedMinutes = 393,
        sleepEfficiency = 0.89,
        habitualFrom = null,
        habitualTo = null,
    )
}
