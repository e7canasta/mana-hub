package com.hub.insights.domain.find

import com.hub.insights.domain.derive.Baseline
import com.hub.insights.inbound.HubOverrideEntry
import com.hub.insights.inbound.HubSceneEvent
import com.hub.insights.inbound.HubSleepDay
import com.hub.insights.domain.rollup.SceneEventTypes
import com.hub.insights.domain.rollup.SceneTimeline
import com.manahive.contracts.scene.PersonState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class FindingCatalogTest {

    private val zone = ZoneId.of("America/Argentina/Buenos_Aires")
    private val ready = Baseline(
        admissionDate = LocalDate.of(2024, 1, 15),
        observedFrom = LocalDate.of(2026, 8, 18),
        observedDays = 30,
        ready = true,
    )

    @Test
    fun `briefing 14d usa la voz de durmio X con salidas en aumento`() {
        val days = sleepWeeks(prevExits = 2, lastExits = listOf(3, 3, 3, 3, 2, 2, 3), calm = 251, restless = 97, awake = 45)
        val derived = exampleDerived(days)
        val text = SleepBriefing.narrative(derived, days)!!
        assertThat(text).isEqualTo(
            "Durmió 5h 48 por noche en promedio, dentro de su rango habitual, " +
                "con 2.7 salidas de cama por noche. Las salidas vienen aumentando respecto de la semana anterior.",
        )
        val cards = SleepBriefing.cards(derived)
        assertThat(cards.map { it.code }).containsExactly("RESTLESS", "BED_EXITS", "TIME_IN_BED", "EFFICIENCY")
        assertThat(cards.single { it.code == "RESTLESS" }.value).isEqualTo("1h 37")
        assertThat(cards.single { it.code == "RESTLESS" }.detail).isEqualTo("28% del total dormido")
        assertThat(cards.single { it.code == "TIME_IN_BED" }.value).isEqualTo("6h 33")
        assertThat(cards.single { it.code == "EFFICIENCY" }.value).isEqualTo("89%")
    }

    @Test
    fun `tres salidas en el alba arman cluster y decision de borde`() {
        val exits = listOf(
            Instant.parse("2026-08-25T08:15:00Z"),
            Instant.parse("2026-08-27T08:40:00Z"),
            Instant.parse("2026-08-30T09:05:00Z"),
        )
        val findings = FindingCatalog.evaluate(
            ctx(
                sleepDays = sleepWeeks(2, 3),
                exits = exits,
                staffAfter = 2,
                riskLevel = "high",
                bedEdgeWarning = 1,
            ),
        )
        val decision = findings.single { it.code == "POLICY_BED_EDGE_DAWN" }
        assertThat(findings.map { it.code }).doesNotContain("BED_EXIT_DAWN_CLUSTER")
        assertThat(decision.awaitingDecision).isTrue()
        assertThat(decision.headline).isEqualTo("Salidas de cama concentradas entre 5 y 6 de la mañana")
        assertThat(decision.body).contains("María salió de la cama tres veces")
        assertThat(decision.body).contains("siempre entre las 5:15 y las 6:05")
        assertThat(decision.body).contains("Dos veces necesitó que fuera una enfermera")
        assertThat(decision.body).contains("Riesgo de caída")
        assertThat(decision.body).doesNotContain("enfermera para el piso")
        assertThat(decision.proposal?.text).contains("Avisar apenas se detecte el borde de la cama")
        assertThat(decision.proposal?.applyLabel).isEqualTo("Aplicar el cambio")
    }

    @Test
    fun `salidas en aumento sin cluster de alba`() {
        val findings = FindingCatalog.evaluate(
            ctx(sleepDays = sleepWeeks(prevExits = 2, lastExits = 4), bedEdgeWarning = null),
        )
        assertThat(findings.map { it.code }).contains("BED_EXITS_RISING")
        assertThat(findings.map { it.code }).doesNotContain("POLICY_BED_EDGE_DAWN", "BED_EXIT_DAWN_CLUSTER")
    }

    @Test
    fun `Susan en formacion no dispara tendencias`() {
        val forming = Baseline(
            admissionDate = LocalDate.of(2026, 8, 30),
            observedFrom = LocalDate.of(2026, 8, 30),
            observedDays = 1,
            ready = false,
        )
        val findings = FindingCatalog.evaluate(
            ctx(
                baseline = forming,
                sleepDays = listOf(HubSleepDay(day = "2026-08-30", calmMinutes = 0, restlessMinutes = 400, bedExitCount = 9)),
                exits = listOf(
                    Instant.parse("2026-08-30T08:10:00Z"),
                    Instant.parse("2026-08-30T08:20:00Z"),
                    Instant.parse("2026-08-30T08:30:00Z"),
                ),
                bedEdgeWarning = 1,
            ),
        )
        assertThat(findings).extracting<String> { it.code }.containsExactly("BASELINE_FORMING")
    }

    @Test
    fun `sin warning de borde el cluster no pide decision`() {
        val exits = listOf(
            Instant.parse("2026-08-25T08:15:00Z"),
            Instant.parse("2026-08-27T08:40:00Z"),
            Instant.parse("2026-08-30T09:05:00Z"),
        )
        val findings = FindingCatalog.evaluate(ctx(exits = exits, bedEdgeWarning = null, riskLevel = "standard"))
        assertThat(findings.map { it.code }).contains("BED_EXIT_DAWN_CLUSTER")
        assertThat(findings.map { it.code }).doesNotContain("POLICY_BED_EDGE_DAWN")
    }

    @Test
    fun `transiciones Lying a Standing cuentan salida`() {
        val events = listOf(
            HubSceneEvent(
                type = SceneEventTypes.TRANSITION,
                from = PersonState.Lying::class.simpleName,
                to = PersonState.Standing::class.simpleName,
                at = Instant.parse("2026-08-30T08:15:00Z"),
            ),
        )
        val exits = BedExits.fromPoints(SceneTimeline.points(events))
        assertThat(exits).containsExactly(Instant.parse("2026-08-30T08:15:00Z"))
        assertThat(BedExits.isDawn(exits.single(), zone)).isTrue()
    }

    @Test
    fun `lo que avisa hoy marca ajuste manual sobre el catalogo`() {
        val lines = PolicyCopy.spokenLines(
            "high",
            mapOf("FALL_RISK_BED_EDGE" to HubOverrideEntry(warningAfterMinutes = 1, alertAfterMinutes = 3)),
        )
        assertThat(lines.any { it.startsWith("Al borde de la cama:") && it.contains("escala a los 3") && it.contains("Ajuste manual") }).isTrue()
        assertThat(lines.any { it.startsWith("Sentado en cama:") && it.contains("15 min") }).isTrue()
        assertThat(PolicyCopy.bedEdgeWarningMinutes("high", emptyMap())).isEqualTo(1)
    }

    private fun ctx(
        baseline: Baseline = ready,
        sleepDays: List<HubSleepDay> = sleepWeeks(2, 3),
        exits: List<Instant> = emptyList(),
        staffAfter: Int = 0,
        riskLevel: String? = "high",
        bedEdgeWarning: Int? = 1,
    ) = FindingContext(
        residentId = "maria",
        residentName = "María López",
        baseline = baseline,
        sleep = exampleDerived(sleepDays),
        sleepDays = sleepDays,
        exitsLast7d = exits,
        staffAfterExitCount = staffAfter,
        riskLevel = riskLevel,
        bedEdgeWarningMinutes = bedEdgeWarning,
        zone = zone,
        windowDays = 14,
        relatedEpisodeIds = listOf("ep-1", "ep-2"),
    )

    private fun exampleDerived(days: List<HubSleepDay>) = com.hub.insights.domain.derive.SleepInsights.derive(days)

    private fun sleepWeeks(
        prevExits: Int,
        lastExits: Int,
        calm: Int = 251,
        restless: Int = 97,
        awake: Int = 45,
    ): List<HubSleepDay> = sleepWeeks(prevExits, List(7) { lastExits }, calm, restless, awake)

    private fun sleepWeeks(
        prevExits: Int,
        lastExits: List<Int>,
        calm: Int = 251,
        restless: Int = 97,
        awake: Int = 45,
    ): List<HubSleepDay> {
        val start = LocalDate.of(2026, 8, 18)
        val last = lastExits.padEnd()
        return (0 until 14).map { n ->
            val exits = if (n < 7) prevExits else last[n - 7]
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

    private fun List<Int>.padEnd(): List<Int> =
        if (size >= 7) take(7) else this + List(7 - size) { last() }
}
