package com.hub.insights.domain.recommend

import com.hub.insights.domain.derive.Baseline
import com.hub.insights.domain.derive.SleepDerived
import com.hub.insights.domain.find.SleepPolicy

data class Recommendation(
    val code: String,
    val severity: String,
    val text: String,
)

object WellbeingRecommendations {

    fun forSleep(baseline: Baseline, derived: SleepDerived, policy: SleepPolicy = SleepPolicy()): List<Recommendation> {
        val out = mutableListOf<Recommendation>()
        if (!baseline.ready) {
            out += Recommendation(
                code = "BASELINE_FORMING",
                severity = "info",
                text = formingCopy(baseline),
            )
            return out
        }

        val share = derived.restlessShare
        when {
            share == null -> Unit
            share <= policy.sleepInRangeThreshold && policy.sleepInRangeEnabled -> out += Recommendation(
                code = "SLEEP_IN_RANGE",
                severity = "info",
                text = "Comparado contra su propia línea base, no contra un estándar: en su rango. " +
                    "Un promedio de sueño sólo significa algo al lado de cuánto duerme habitualmente esta persona.",
            )
            share <= policy.restlessFragmentedThreshold && policy.restlessFragmentedEnabled -> out += Recommendation(
                code = "SLEEP_RESTLESS",
                severity = "warning",
                text = "El sueño inquieto está por encima de su rango habitual. " +
                    "Conviene mirar salidas de cama y ventanas cortadas antes de cambiar alarmas.",
            )
            policy.restlessFragmentedEnabled -> out += Recommendation(
                code = "SLEEP_FRAGMENTED",
                severity = "warning",
                text = "Noche muy fragmentada respecto de su línea base. Revisar perfil ComeBack y rondas de madrugada.",
            )
        }

        val delta = derived.deltaCalmMinutesWoW
        if (delta != null && delta <= -policy.dropWoWMinutes && policy.dropWoWEnabled) {
            out += Recommendation(
                code = "SLEEP_DROP_WOW",
                severity = "warning",
                text = "Duerme bastante menos que la semana anterior (${formatMinutes(delta)}). No es una alerta de umbral fijo: es un cambio contra sí mismo.",
            )
        }
        return out
    }

    fun forCare(baseline: Baseline, avgMinutes: Double?, totalMinutes: Int): List<Recommendation> {
        if (!baseline.ready) {
            return listOf(
                Recommendation(
                    code = "CARE_BASELINE_FORMING",
                    severity = "info",
                    text = formingCopy(baseline) + " El gráfico en cero no es una caída de actividad: todavía no hay días suficientes.",
                ),
            )
        }
        if (avgMinutes == null) {
            return listOf(
                Recommendation(
                    code = "CARE_NOT_MEASURED",
                    severity = "info",
                    text = "Todavía no hay un rollup de cuidado para estos días. No es cero de visitas.",
                ),
            )
        }
        if (totalMinutes == 0 || avgMinutes == 0.0) {
            return listOf(
                Recommendation(
                    code = "CARE_NONE",
                    severity = "info",
                    text = "Sin visitas de cuidado registradas en la ventana. Cero medido, no un error de carga.",
                ),
            )
        }
        return emptyList()
    }

    fun forEpisodeResolved(selfRecovery: Boolean, durationMinutes: Int?): List<Recommendation> {
        if (selfRecovery) {
            return listOf(
                Recommendation(
                    code = "EPISODE_SELF_RECOVERY",
                    severity = "info",
                    text = "El episodio se cerró porque volvió solo al estado seguro" +
                        (durationMinutes?.let { " (≈ ${formatMinutes(it)})" } ?: "") +
                        ". No sustituye el resumen de sueño de la noche: eso se cierra en el rollup diario.",
                ),
            )
        }
        return listOf(
            Recommendation(
                code = "EPISODE_STAFF_CLOSED",
                severity = "info",
                text = "Episodio resuelto con intervención. Si hubo staff en habitación, cuenta como cuidado reactivo — no como ronda.",
            ),
        )
    }

    private fun formingCopy(baseline: Baseline): String {
        val days = baseline.observedDays
        val needed = 7
        return "Alta hace $days día${if (days == 1) "" else "s"}. " +
            "Línea base en formación (hacen falta $needed días). No evaluar tendencias ni umbrales todavía."
    }
}

fun formatMinutes(total: Int): String {
    val sign = if (total < 0) "−" else if (total > 0) "+" else ""
    val abs = kotlin.math.abs(total)
    val h = abs / 60
    val m = abs % 60
    return if (h == 0) "${sign}${m}m" else "${sign}${h}h ${m.toString().padStart(2, '0')}"
}
