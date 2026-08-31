package com.hub.insights.find

import com.hub.insights.derive.SleepDerived
import com.hub.insights.inbound.HubSleepDay
import kotlin.math.abs

object SleepBriefing {

    fun cards(derived: SleepDerived): List<KpiCard> {
        val out = mutableListOf<KpiCard>()
        derived.avgRestlessMinutes7d?.let { minutes ->
            val share = derived.restlessShare?.let { "${CopyFormat.percent(it)} del total dormido" }
            out += KpiCard(
                code = "RESTLESS",
                label = "Tiempo en sueño inquieto",
                value = CopyFormat.clock(minutes),
                detail = share,
            )
        }
        derived.avgBedExits?.let { avg ->
            val max = derived.maxBedExits?.let { "máximo en la ventana: $it" }
            out += KpiCard(
                code = "BED_EXITS",
                label = "Salidas de cama por noche",
                value = CopyFormat.oneDecimal(avg),
                detail = max,
            )
        }
        derived.avgTimeInBedMinutes?.let { minutes ->
            out += KpiCard(
                code = "TIME_IN_BED",
                label = "Tiempo total en cama",
                value = CopyFormat.clock(minutes),
                detail = "incluye despertares",
            )
        }
        derived.sleepEfficiency?.let { eff ->
            out += KpiCard(
                code = "EFFICIENCY",
                label = "Eficiencia de sueño",
                value = CopyFormat.percent(eff),
                detail = "dormido sobre tiempo en cama",
            )
        }
        return out
    }

    fun narrative(derived: SleepDerived, sleepDays: List<HubSleepDay>): String? {
        val asleep = derived.avgAsleepMinutes7d ?: return null
        val range = rangeClause(asleep, sleepDays)
        val exits = derived.avgBedExits?.let { ", con ${CopyFormat.oneDecimal(it)} salidas de cama por noche" } ?: ""
        val rising = if (exitsRising(sleepDays)) " Las salidas vienen aumentando respecto de la semana anterior." else ""
        return "Durmió ${CopyFormat.clock(asleep)} por noche en promedio$range$exits.$rising"
    }

    fun rangeClause(asleepMinutes: Int, sleepDays: List<HubSleepDay>): String {
        val median = medianAsleep(sleepDays) ?: return ""
        val delta = asleepMinutes - median
        return when {
            abs(delta) <= 45 -> ", dentro de su rango habitual"
            delta < 0 -> ", por debajo de su rango habitual"
            else -> ", por encima de su rango habitual"
        }
    }

    fun exitsRising(sleepDays: List<HubSleepDay>): Boolean {
        val (last7, prev7) = weeks(sleepDays)
        if (last7.isEmpty() || prev7.isEmpty()) return false
        val last = last7.map { it.bedExitCount }.average()
        val prev = prev7.map { it.bedExitCount }.average()
        return last >= prev * 1.15 && last - prev >= 0.3
    }

    fun weeks(sleepDays: List<HubSleepDay>): Pair<List<HubSleepDay>, List<HubSleepDay>> {
        val ordered = sleepDays.filter { it.measured }.sortedBy { it.day }
        val last7 = ordered.takeLast(7)
        val prev7 = ordered.dropLast(7).takeLast(7)
        return last7 to prev7
    }

    private fun medianAsleep(sleepDays: List<HubSleepDay>): Int? {
        val values = sleepDays.filter { it.measured }
            .map { it.calmMinutes + it.restlessMinutes }
            .sorted()
        if (values.isEmpty()) return null
        return values[values.size / 2]
    }
}
