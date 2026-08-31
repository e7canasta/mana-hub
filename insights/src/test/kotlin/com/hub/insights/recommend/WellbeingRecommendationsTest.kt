package com.hub.insights.recommend

import com.hub.insights.derive.Baseline
import com.hub.insights.derive.SleepDerived
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class WellbeingRecommendationsTest {

    @Test
    fun `Susan en formacion no dispara umbrales de sueno inquieto`() {
        val baseline = Baseline(
            admissionDate = LocalDate.of(2026, 8, 30),
            observedFrom = LocalDate.of(2026, 8, 30),
            observedDays = 1,
            ready = false,
        )
        val derived = SleepDerived(
            avgCalmMinutes7d = 0,
            deltaCalmMinutesWoW = null,
            avgRestlessMinutes7d = 0,
            restlessShare = 0.9,
            avgBedExits = 0.0,
            maxBedExits = 0,
            avgTimeInBedMinutes = 0,
            sleepEfficiency = null,
            habitualFrom = null,
            habitualTo = null,
        )
        val recs = WellbeingRecommendations.forSleep(baseline, derived)
        assertThat(recs).extracting<String> { it.code }.containsExactly("BASELINE_FORMING")
    }

    @Test
    fun `episodio E1 vuelve solo no es rollup de sueno`() {
        val recs = WellbeingRecommendations.forEpisodeResolved(selfRecovery = true, durationMinutes = 17)
        assertThat(recs.single().code).isEqualTo("EPISODE_SELF_RECOVERY")
        assertThat(recs.single().text).contains("rollup")
    }
}
