package com.hub.insights.derive

import com.hub.insights.inbound.HubSleepDay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SleepInsightsTest {

    @Test
    fun `promedio 7d y delta vs semana previa`() {
        val days = (1..14).map { n ->
            val day = LocalDate.of(2026, 8, 18).plusDays(n.toLong() - 1)
            val calm = if (n <= 7) 400 else 436
            HubSleepDay(
                day = day.toString(),
                calmMinutes = calm,
                restlessMinutes = 68,
                awakeMinutes = 20,
                outOfBedMinutes = 10,
                bedExitCount = 3,
            )
        }
        val d = SleepInsights.derive(days)
        assertThat(d.avgCalmMinutes7d).isEqualTo(436)
        assertThat(d.deltaCalmMinutesWoW).isEqualTo(36)
        assertThat(d.restlessShare).isGreaterThan(0.10)
        assertThat(d.restlessShare).isLessThan(0.20)
        assertThat(d.avgBedExits).isEqualTo(3.0)
        assertThat(d.sleepEfficiency).isGreaterThan(0.90)
    }

    @Test
    fun `una sola noche no inventa delta wow`() {
        val d = SleepInsights.derive(
            listOf(HubSleepDay(day = "2026-08-30", calmMinutes = 350, restlessMinutes = 40)),
        )
        assertThat(d.avgCalmMinutes7d).isEqualTo(350)
        assertThat(d.deltaCalmMinutesWoW).isNull()
    }

    @Test
    fun `dias no medidos no tiran el promedio a cero`() {
        val days = listOf(
            HubSleepDay(day = "2026-08-29", calmMinutes = 400, measured = true),
            HubSleepDay(day = "2026-08-30", calmMinutes = 0, measured = false),
            HubSleepDay(day = "2026-08-31", calmMinutes = 0, measured = false),
        )
        val d = SleepInsights.derive(days)
        assertThat(d.avgCalmMinutes7d).isEqualTo(400)
    }
}
