package com.hub.insights.domain.find

import com.hub.insights.domain.derive.Baseline
import com.hub.insights.domain.derive.SleepInsights
import com.hub.insights.inbound.HubBathroomDay
import com.hub.insights.inbound.HubOverrideEntry
import com.hub.insights.inbound.HubSleepDay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * FindingCatalog — el motor de reglas que genera hallazgos.
 *
 * Cada regla analiza una dimensión del bienestar del residente y produce
 * un "finding" que el director médico puede revisar. Los hallazgos tienen
 * código, tipo, polaridad y severidad — el panel los agrupa por urgencia.
 */
class FindingCatalogSpec {

    private val zone = ZoneId.of("America/Argentina/Buenos_Aires")
    private val ready = Baseline(
        admissionDate = LocalDate.of(2024, 1, 15),
        observedFrom = LocalDate.of(2026, 8, 18),
        observedDays = 30,
        ready = true,
    )

    @Nested
    inner class `Linea base en formacion - primeros 7 dias` {

        @Test
        fun `residente recien llegado solo muestra BASELINE_FORMING`() {
            val forming = Baseline(
                admissionDate = LocalDate.of(2026, 8, 30),
                observedFrom = LocalDate.of(2026, 8, 30),
                observedDays = 1,
                ready = false,
            )
            val findings = FindingCatalog.evaluate(ctx(baseline = forming))
            assertThat(findings).hasSize(1)
            assertThat(findings.first().code).isEqualTo("BASELINE_FORMING")
            assertThat(findings.first().kind).isEqualTo(FindingKind.WATCH)
            assertThat(findings.first().polarity).isEqualTo(Polarity.NEUTRAL)
        }

        @Test
        fun `dia singular en el body`() {
            val forming = Baseline(
                admissionDate = LocalDate.of(2026, 8, 30),
                observedFrom = LocalDate.of(2026, 8, 30),
                observedDays = 1,
                ready = false,
            )
            val findings = FindingCatalog.evaluate(ctx(baseline = forming))
            assertThat(findings.first().body).contains("1 día")
            assertThat(findings.first().body).doesNotContain("1 días")
        }

        @Test
        fun `dia plural en el body`() {
            val forming = Baseline(
                admissionDate = LocalDate.of(2026, 8, 28),
                observedFrom = LocalDate.of(2026, 8, 28),
                observedDays = 3,
                ready = false,
            )
            val findings = FindingCatalog.evaluate(ctx(baseline = forming))
            assertThat(findings.first().body).contains("3 días")
        }
    }

    @Nested
    inner class `Cluster de alba - salidas concentradas 05 00 a 06 05` {

        @Test
        fun `cluster con todas las salidas en alba produce BED_EXIT_DAWN_CLUSTER`() {
            val exits = dawnExits(3)
            val findings = FindingCatalog.evaluate(ctx(exits = exits, bedEdgeWarning = null))
            val cluster = findings.single { it.code == "BED_EXIT_DAWN_CLUSTER" }
            assertThat(cluster.kind).isEqualTo(FindingKind.CLUSTER)
            assertThat(cluster.polarity).isEqualTo(Polarity.CONCERN)
            assertThat(cluster.severity).isEqualTo("warning")
        }

        @Test
        fun `cluster con todas las salidas en alba dice siempre entre`() {
            val exits = dawnExits(3)
            val findings = FindingCatalog.evaluate(ctx(exits = exits, bedEdgeWarning = null))
            assertThat(findings.single { it.code == "BED_EXIT_DAWN_CLUSTER" }.body)
                .contains("siempre entre las")
        }

        @Test
        fun `cluster mixto - solo algunas en alba dice concentradas entre`() {
            val exits = listOf(
                Instant.parse("2026-08-25T08:15:00Z"), // 05:15 ART — alba
                Instant.parse("2026-08-27T08:40:00Z"), // 05:40 ART — alba
                Instant.parse("2026-08-30T12:00:00Z"), // 09:00 ART — NO alba
            )
            val findings = FindingCatalog.evaluate(ctx(exits = exits, bedEdgeWarning = null))
            val cluster = findings.single { it.code == "BED_EXIT_DAWN_CLUSTER" }
            assertThat(cluster.body).contains("concentradas entre las")
            assertThat(cluster.body).doesNotContain("siempre entre las")
        }

        @Test
        fun `menos de 3 salidas no forma cluster`() {
            val exits = dawnExits(2)
            val findings = FindingCatalog.evaluate(ctx(exits = exits, bedEdgeWarning = null))
            assertThat(findings.map { it.code }).doesNotContain("BED_EXIT_DAWN_CLUSTER")
        }

        @Test
        fun `cluster sin warning de borde no pide decision`() {
            val exits = dawnExits(3)
            val findings = FindingCatalog.evaluate(ctx(exits = exits, bedEdgeWarning = null))
            assertThat(findings.map { it.code }).doesNotContain("POLICY_BED_EDGE_DAWN")
        }

        @Test
        fun `cluster con warning de borde produce POLICY_BED_EDGE_DAWN`() {
            val exits = dawnExits(3)
            val findings = FindingCatalog.evaluate(ctx(exits = exits, bedEdgeWarning = 1, riskLevel = "high"))
            val decision = findings.single { it.code == "POLICY_BED_EDGE_DAWN" }
            assertThat(decision.awaitingDecision).isTrue()
            assertThat(decision.proposal).isNotNull()
            assertThat(decision.proposal?.action).isEqualTo("SET_BED_EDGE_WARNING_IMMEDIATE")
        }

        @Test
        fun `cluster con staff despues de salida incluye oracion de enfermera`() {
            val exits = dawnExits(3)
            val findings = FindingCatalog.evaluate(
                ctx(exits = exits, staffAfter = 2, bedEdgeWarning = 1, riskLevel = "high"),
            )
            assertThat(findings.single { it.code == "POLICY_BED_EDGE_DAWN" }.body)
                .contains("Dos veces necesitó que fuera una enfermera")
        }

        @Test
        fun `cluster con staff cero no menciona enfermera`() {
            val exits = dawnExits(3)
            val findings = FindingCatalog.evaluate(
                ctx(exits = exits, staffAfter = 0, bedEdgeWarning = 1, riskLevel = "high"),
            )
            assertThat(findings.single { it.code == "POLICY_BED_EDGE_DAWN" }.body)
                .doesNotContain("enfermera")
        }
    }

    @Nested
    inner class `Salidas de cama en aumento` {

        @Test
        fun `salidas suben respecto de la semana anterior`() {
            val days = sleepWeeks(prevExits = 2, lastExits = listOf(3, 3, 3, 3, 3, 3, 3))
            val findings = FindingCatalog.evaluate(ctx(sleepDays = days, bedEdgeWarning = null))
            assertThat(findings.map { it.code }).contains("BED_EXITS_RISING")
        }

        @Test
        fun `salidas estables no generan hallazgo`() {
            val days = sleepWeeks(prevExits = 3, lastExits = listOf(3, 3, 3, 3, 3, 3, 3))
            val findings = FindingCatalog.evaluate(ctx(sleepDays = days, bedEdgeWarning = null))
            assertThat(findings.map { it.code }).doesNotContain("BED_EXITS_RISING")
        }

        @Test
        fun `hallazgo de salidas en aumento tiene evidencia`() {
            val days = sleepWeeks(prevExits = 2, lastExits = listOf(3, 3, 3, 3, 3, 3, 3))
            val findings = FindingCatalog.evaluate(ctx(sleepDays = days, bedEdgeWarning = null))
            val finding = findings.single { it.code == "BED_EXITS_RISING" }
            assertThat(finding.evidence).containsKeys("last7", "prev7")
        }
    }

    @Nested
    inner class `Sueño inquieto alto` {

        @Test
        fun `restless share mayor a 25 por ciento genera SLEEP_RESTLESS_HIGH`() {
            val days = sleepDays(calm = 200, restless = 100, awake = 50)
            val findings = FindingCatalog.evaluate(ctx(sleepDays = days, bedEdgeWarning = null))
            assertThat(findings.map { it.code }).contains("SLEEP_RESTLESS_HIGH")
        }

        @Test
        fun `restless share exactamente 25 por ciento no genera hallazgo`() {
            val days = sleepDays(calm = 300, restless = 100, awake = 0)
            val findings = FindingCatalog.evaluate(ctx(sleepDays = days, bedEdgeWarning = null))
            assertThat(findings.map { it.code }).doesNotContain("SLEEP_RESTLESS_HIGH")
        }

        @Test
        fun `hallazgo de sueño inquieto tiene polaridad CONCERN`() {
            val days = sleepDays(calm = 200, restless = 100, awake = 50)
            val findings = FindingCatalog.evaluate(ctx(sleepDays = days, bedEdgeWarning = null))
            val finding = findings.single { it.code == "SLEEP_RESTLESS_HIGH" }
            assertThat(finding.polarity).isEqualTo(Polarity.CONCERN)
        }
    }

    @Nested
    inner class `Sueño dentro del rango` {

        @Test
        fun `restless share menor o igual a 20 por ciento genera SLEEP_IN_RANGE`() {
            val days = sleepDays(calm = 350, restless = 50, awake = 0)
            val findings = FindingCatalog.evaluate(ctx(sleepDays = days, bedEdgeWarning = null))
            assertThat(findings.map { it.code }).contains("SLEEP_IN_RANGE")
        }

        @Test
        fun `hallazgo de sueño en rango tiene polaridad POSITIVE`() {
            val days = sleepDays(calm = 350, restless = 50, awake = 0)
            val findings = FindingCatalog.evaluate(ctx(sleepDays = days, bedEdgeWarning = null))
            val finding = findings.single { it.code == "SLEEP_IN_RANGE" }
            assertThat(finding.polarity).isEqualTo(Polarity.POSITIVE)
        }

        @Test
        fun `restless share entre 20 y 25 no genera ninguno de los dos`() {
            val days = sleepDays(calm = 300, restless = 80, awake = 20)
            val findings = FindingCatalog.evaluate(ctx(sleepDays = days, bedEdgeWarning = null))
            assertThat(findings.map { it.code }).doesNotContain("SLEEP_IN_RANGE")
            assertThat(findings.map { it.code }).doesNotContain("SLEEP_RESTLESS_HIGH")
        }
    }

    @Nested
    inner class `Visitas nocturnas al baño en aumento` {

        @Test
        fun `banos nocturnos suben mas de una vez y media genera BATHROOM_NIGHT_UP`() {
            val bathroomDays = (1..14).map { n ->
                val count = if (n <= 7) 1 else 3
                HubBathroomDay(day = "2026-08-${18 + n}", nightVisitCount = count, measured = true)
            }
            val findings = FindingCatalog.evaluate(
                ctx(sleepDays = sleepWeeks(2, 2), bedEdgeWarning = null, bathroomDays = bathroomDays),
            )
            assertThat(findings.map { it.code }).contains("BATHROOM_NIGHT_UP")
        }

        @Test
        fun `banos nocturnos estables no generan hallazgo`() {
            val bathroomDays = (1..14).map { n ->
                HubBathroomDay(day = "2026-08-${18 + n}", nightVisitCount = 2, measured = true)
            }
            val findings = FindingCatalog.evaluate(
                ctx(sleepDays = sleepWeeks(2, 2), bedEdgeWarning = null, bathroomDays = bathroomDays),
            )
            assertThat(findings.map { it.code }).doesNotContain("BATHROOM_NIGHT_UP")
        }

        @Test
        fun `menos de 1 bano por noche promedio no genera hallazgo`() {
            val bathroomDays = (1..14).map { n ->
                HubBathroomDay(day = "2026-08-${18 + n}", nightVisitCount = if (n <= 7) 0 else if (n % 2 == 0) 1 else 0, measured = true)
            }
            val findings = FindingCatalog.evaluate(
                ctx(sleepDays = sleepWeeks(2, 2), bedEdgeWarning = null, bathroomDays = bathroomDays),
            )
            assertThat(findings.map { it.code }).doesNotContain("BATHROOM_NIGHT_UP")
        }
    }

    @Nested
    inner class `Poco cuidado medido` {

        @Test
        fun `promedio menor a 20 minutos genera CARE_THIN`() {
            val findings = FindingCatalog.evaluate(
                ctx(sleepDays = sleepWeeks(2, 2), bedEdgeWarning = null, careAvgMinutes = 15.0),
            )
            assertThat(findings.map { it.code }).contains("CARE_THIN")
        }

        @Test
        fun `promedio de 20 minutos no genera hallazgo`() {
            val findings = FindingCatalog.evaluate(
                ctx(sleepDays = sleepWeeks(2, 2), bedEdgeWarning = null, careAvgMinutes = 20.0),
            )
            assertThat(findings.map { it.code }).doesNotContain("CARE_THIN")
        }

        @Test
        fun `sin datos de cuidado no genera hallazgo`() {
            val findings = FindingCatalog.evaluate(
                ctx(sleepDays = sleepWeeks(2, 2), bedEdgeWarning = null, careAvgMinutes = null),
            )
            assertThat(findings.map { it.code }).doesNotContain("CARE_THIN")
        }

        @Test
        fun `hallazgo de poco cuidado tiene evidencia con minutos`() {
            val findings = FindingCatalog.evaluate(
                ctx(sleepDays = sleepWeeks(2, 2), bedEdgeWarning = null, careAvgMinutes = 12.5),
            )
            val finding = findings.single { it.code == "CARE_THIN" }
            assertThat(finding.evidence["avgMinutesPerDay"]).isEqualTo(12.5)
        }
    }

    @Nested
    inner class `Narrativa de sueño en el briefing` {

        @Test
        fun `residente con sueño estable genera SLEEP_14D_BRIEFING`() {
            val days = sleepDays(calm = 251, restless = 97, awake = 45, exits = 3)
            val findings = FindingCatalog.evaluate(ctx(sleepDays = days, bedEdgeWarning = null))
            assertThat(findings.map { it.code }).contains("SLEEP_14D_BRIEFING")
        }

        @Test
        fun `briefing tiene tipo BRIEFING y polaridad NEUTRAL`() {
            val days = sleepDays(calm = 251, restless = 97, awake = 45, exits = 3)
            val findings = FindingCatalog.evaluate(ctx(sleepDays = days, bedEdgeWarning = null))
            val briefing = findings.single { it.code == "SLEEP_14D_BRIEFING" }
            assertThat(briefing.kind).isEqualTo(FindingKind.BRIEFING)
            assertThat(briefing.polarity).isEqualTo(Polarity.NEUTRAL)
        }

        @Test
        fun `briefing con header de ventana`() {
            val days = sleepDays(calm = 251, restless = 97, awake = 45, exits = 3)
            val findings = FindingCatalog.evaluate(ctx(sleepDays = days, bedEdgeWarning = null, windowDays = 14))
            val briefing = findings.single { it.code == "SLEEP_14D_BRIEFING" }
            assertThat(briefing.headline).contains("14 días")
        }
    }

    @Nested
    inner class `Multiples hallazgos simultaneos` {

        @Test
        fun `residente con cluster, salidas en aumento y sueño inquieto genera los 3`() {
            val exits = dawnExits(3)
            val days = sleepWeeks(prevExits = 2, lastExits = listOf(3, 3, 3, 3, 3, 3, 3), restless = 150)
            val findings = FindingCatalog.evaluate(
                ctx(sleepDays = days, exits = exits, bedEdgeWarning = null),
            )
            val codes = findings.map { it.code }
            assertThat(codes).contains("BED_EXIT_DAWN_CLUSTER")
            assertThat(codes).contains("BED_EXITS_RISING")
            assertThat(codes).contains("SLEEP_RESTLESS_HIGH")
        }
    }

    private fun ctx(
        baseline: Baseline = ready,
        sleepDays: List<HubSleepDay> = sleepWeeks(2, 3),
        exits: List<Instant> = emptyList(),
        staffAfter: Int = 0,
        riskLevel: String? = "high",
        bedEdgeWarning: Int? = 1,
        careAvgMinutes: Double? = null,
        bathroomDays: List<HubBathroomDay> = emptyList(),
        windowDays: Int = 14,
    ) = FindingContext(
        residentId = "maria",
        residentName = "María López",
        baseline = baseline,
        sleep = SleepInsights.derive(sleepDays),
        sleepDays = sleepDays,
        bathroomDays = bathroomDays,
        careAvgMinutes = careAvgMinutes,
        exitsLast7d = exits,
        staffAfterExitCount = staffAfter,
        riskLevel = riskLevel,
        bedEdgeWarningMinutes = bedEdgeWarning,
        zone = zone,
        windowDays = windowDays,
    )

    private fun dawnExits(n: Int): List<Instant> = (0 until n).map { i ->
        Instant.parse("2026-08-${25 + i}T08:${15 + i * 10}:00Z") // 05:15+, ART
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
        val start = LocalDate.of(2026, 8, 18)
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

    private fun sleepWeeks(prevExits: Int, lastExits: Int) =
        sleepWeeks(prevExits, List(7) { lastExits })
}
