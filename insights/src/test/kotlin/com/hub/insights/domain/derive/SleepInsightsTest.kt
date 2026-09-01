package com.hub.insights.domain.derive

import com.hub.insights.inbound.HubSleepDay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

class SleepInsightsTest {

    @Nested
    inner class `SleepInsights derive` {

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

        @Test
        fun `lista vacia retorna todo null`() {
            val d = SleepInsights.derive(emptyList())
            assertThat(d.avgCalmMinutes7d).isNull()
            assertThat(d.deltaCalmMinutesWoW).isNull()
            assertThat(d.restlessShare).isNull()
            assertThat(d.sleepEfficiency).isNull()
        }

        @Test
        fun `restlessShare es cero cuando no hay restless`() {
            val days = (1..7).map { n ->
                HubSleepDay(day = "2026-08-${18 + n}", calmMinutes = 300, restlessMinutes = 0)
            }
            val d = SleepInsights.derive(days)
            assertThat(d.restlessShare).isEqualTo(0.0)
        }

        @Test
        fun `sleepEfficiency es cero cuando inBed es cero`() {
            val days = (1..7).map { n ->
                HubSleepDay(day = "2026-08-${18 + n}", calmMinutes = 0, restlessMinutes = 0, awakeMinutes = 0)
            }
            val d = SleepInsights.derive(days)
            assertThat(d.sleepEfficiency).isNull()
        }

        @Test
        fun `habitual times se calculan con mediana`() {
            val start = LocalDate.of(2026, 8, 1)
            val days = (0 until 14).map { n ->
                HubSleepDay(
                    day = start.plusDays(n.toLong()).toString(),
                    calmMinutes = 300,
                    startedAt = start.plusDays(n.toLong()).atTime(22, 0),
                    endedAt = start.plusDays(n.toLong() + 1).atTime(6, 30),
                )
            }
            val d = SleepInsights.derive(days)
            assertThat(d.habitualFrom).isNotNull()
            assertThat(d.habitualTo).isNotNull()
        }

        @Test
        fun `sin startedAt habitualFrom es null`() {
            val days = (1..7).map { n ->
                HubSleepDay(day = "2026-08-${18 + n}", calmMinutes = 300)
            }
            val d = SleepInsights.derive(days)
            assertThat(d.habitualFrom).isNull()
            assertThat(d.habitualTo).isNull()
        }
    }

}
