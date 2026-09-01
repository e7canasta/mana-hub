package com.hub.insights.experts

import com.hub.insights.domain.derive.Baseline
import com.hub.insights.domain.derive.SleepDerived
import com.hub.insights.engine.InsightContext
import com.hub.insights.engine.InsightEngine
import com.hub.insights.engine.BathroomDayData
import com.hub.insights.engine.EpisodeData
import com.hub.insights.engine.SleepDayData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

class ExpertEngineTest {

    private val zone = ZoneId.of("America/Argentina/Buenos_Aires")

    @Nested
    inner class `sleep expert` {

        @Test
        fun `genera findings de sueño`() {
            val context = ctx(sleepDays = sleepDays(calm = 251, restless = 97, awake = 45))
            val result = com.hub.insights.experts.SleepExpert().evaluate(context)
            assertThat(result.expertName).isEqualTo("sueño")
            assertThat(result.findings).isNotEmpty()
        }

        @Test
        fun `genera recomendaciones de sueño`() {
            val context = ctx(sleepDays = sleepDays(calm = 251, restless = 97, awake = 45))
            val result = com.hub.insights.experts.SleepExpert().evaluate(context)
            assertThat(result.recommendations).isNotEmpty()
        }
    }

    @Nested
    inner class `care expert` {

        @Test
        fun `con datos de cuidado no genera recomendaciones`() {
            val context = ctx(careAvgMinutes = 15.0, careTotalMinutes = 200)
            val result = com.hub.insights.experts.CareExpert().evaluate(context)
            assertThat(result.expertName).isEqualTo("cuidado")
            assertThat(result.recommendations).isEmpty()
        }

        @Test
        fun `sin datos de cuidado genera CARE_NOT_MEASURED`() {
            val context = ctx(careAvgMinutes = null, careTotalMinutes = 0)
            val result = com.hub.insights.experts.CareExpert().evaluate(context)
            assertThat(result.recommendations.any { it.code == "CARE_NOT_MEASURED" }).isTrue()
        }
    }

    @Nested
    inner class `engine ensambla` {

        @Test
        fun `resultados de todos los expertos`() {
            val context = ctx(sleepDays = sleepDays(calm = 251, restless = 97, awake = 45))
            val engine = com.hub.insights.engine.InsightEngine()
            val result = engine.evaluate(context)
            assertThat(result.expertResults).hasSize(3)
            assertThat(result.findings).isNotEmpty()
            assertThat(result.recommendations).isNotEmpty()
        }

        @Test
        fun `resultados agrupados por experto`() {
            val context = ctx(sleepDays = sleepDays(calm = 251, restless = 97, awake = 45))
            val result = com.hub.insights.engine.InsightEngine().evaluate(context)
            val expertNames = result.expertResults.map { it.expertName }
            assertThat(expertNames).containsExactly("sueño", "cuidado", "política")
        }
    }

    private fun ctx(
        sleepDays: List<SleepDayData> = sleepDays(),
        careAvgMinutes: Double? = null,
        careTotalMinutes: Int = 0,
    ) = InsightContext(
        residentId = "jose",
        residentName = "José García",
        from = LocalDate.of(2026, 8, 1),
        to = LocalDate.of(2026, 8, 30),
        baseline = Baseline(
            admissionDate = LocalDate.of(2024, 1, 15),
            observedFrom = LocalDate.of(2026, 8, 1),
            observedDays = 30,
            ready = true,
        ),
        derived = com.hub.insights.domain.derive.SleepInsights.derive(
            sleepDays.map {
                com.hub.insights.inbound.HubSleepDay(
                    day = it.day,
                    calmMinutes = it.calmMinutes,
                    restlessMinutes = it.restlessMinutes,
                    awakeMinutes = it.awakeMinutes,
                    bedExitCount = it.bedExitCount,
                    measured = it.measured,
                )
            }
        ),
        sleepDays = sleepDays,
        bathroomDays = emptyList(),
        careAvgMinutes = careAvgMinutes,
        careTotalMinutes = careTotalMinutes,
        exitsLast7d = emptyList(),
        staffAfterExitCount = 0,
        riskLevel = "high",
        bedEdgeWarningMinutes = 1,
        relatedEpisodeIds = emptyList(),
        policyToday = emptyList(),
        episodes = emptyList(),
        zone = zone,
        windowDays = 30,
    )

    private fun sleepDays(
        calm: Int = 251,
        restless: Int = 97,
        awake: Int = 45,
    ): List<SleepDayData> = (1..14).map { n ->
        SleepDayData(
            day = "2026-08-${18 + n}",
            calmMinutes = calm,
            restlessMinutes = restless,
            awakeMinutes = awake,
            bedExitCount = 3,
        )
    }
}
