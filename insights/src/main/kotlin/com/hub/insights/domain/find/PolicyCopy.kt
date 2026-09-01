package com.hub.insights.domain.find

import com.hub.insights.inbound.HubOverrideEntry

/**
 * Copy coloquial de “lo que avisa hoy”.
 *
 * Espejo director-facing de [com.hub.policy.domain.model.DagCatalogs] — insights
 * no depende del módulo policy. Los números salen del catálogo del nivel más
 * los overrides reales del hub; no se inventan umbrales de UI.
 */
object PolicyCopy {

    data class SpokenRule(
        val state: String,
        val label: String,
        val warningAfterMinutes: Int?,
        val alertAfterMinutes: Int?,
        val manual: Boolean,
    ) {
        fun line(): String {
            if (warningAfterMinutes == null) {
                return "$label: sin aviso de permanencia."
            }
            val scale = alertAfterMinutes?.let { ", y escala a los $it." } ?: "."
            val note = if (manual) " Ajuste manual." else ""
            return "$label: avisa a los $warningAfterMinutes min$scale$note"
        }
    }

    private data class Dwell(val warning: Int?, val alert: Int?)

    private val shown = listOf(
        "SITTING_IN_BED" to "Sentado en cama",
        "BED_EDGE" to "Al borde de la cama",
        "STANDING" to "De pie",
        "ABSENT" to "Sin observación",
    )

    /** FALL_RISK — DagCatalogs. */
    private val fallRisk = mapOf(
        "SITTING_IN_BED" to Dwell(15, 20),
        "BED_EDGE" to Dwell(1, 2),
        "STANDING" to Dwell(2, 3),
        "ABSENT" to Dwell(5, 10),
    )

    /** NIGHT_WANDERING — DagCatalogs. */
    private val nightWandering = mapOf(
        "SITTING_IN_BED" to Dwell(20, 30),
        "BED_EDGE" to Dwell(3, 5),
        "STANDING" to Dwell(10, 15),
        "ABSENT" to Dwell(5, 10),
    )

    /** CRITICAL — DagCatalogs. */
    private val critical = mapOf(
        "SITTING_IN_BED" to Dwell(10, 15),
        "BED_EDGE" to Dwell(1, 2),
        "STANDING" to Dwell(2, 3),
        "ABSENT" to Dwell(2, 5),
    )

    fun levelLabel(riskLevel: String?): String? = when (riskLevel?.lowercase()) {
        "high", "fall_risk" -> "Riesgo de caída"
        "medium", "night_wandering" -> "Deambulación nocturna"
        "low", "standard" -> "Vigilancia baja"
        "critical" -> "Crítico"
        else -> riskLevel
    }

    fun spokenRules(riskLevel: String?, overrides: Map<String, HubOverrideEntry>): List<SpokenRule> {
        val catalog = catalogOf(riskLevel)
        return shown.map { (state, label) ->
            val over = overrideFor(overrides, state)
            val base = catalog[state]
            SpokenRule(
                state = state,
                label = label,
                warningAfterMinutes = over?.warningAfterMinutes ?: base?.warning,
                alertAfterMinutes = over?.alertAfterMinutes ?: base?.alert,
                manual = over?.warningAfterMinutes != null || over?.alertAfterMinutes != null,
            )
        }
    }

    fun spokenLines(riskLevel: String?, overrides: Map<String, HubOverrideEntry>): List<String> =
        spokenRules(riskLevel, overrides).map { it.line() }

    fun bedEdgeWarningMinutes(riskLevel: String?, overrides: Map<String, HubOverrideEntry>): Int? {
        val over = overrideFor(overrides, "BED_EDGE")
        if (over?.warningAfterMinutes != null) return over.warningAfterMinutes
        return catalogOf(riskLevel)["BED_EDGE"]?.warning
    }

    private fun catalogOf(riskLevel: String?): Map<String, Dwell> = when (riskLevel?.lowercase()) {
        "high", "fall_risk" -> fallRisk
        "medium", "night_wandering" -> nightWandering
        "critical" -> critical
        else -> emptyMap()
    }

    private fun overrideFor(overrides: Map<String, HubOverrideEntry>, state: String): HubOverrideEntry? {
        val tokens = when (state) {
            "SITTING_IN_BED" -> listOf("SITTING_IN_BED", "SITTING")
            "BED_EDGE" -> listOf("BED_EDGE")
            "STANDING" -> listOf("STANDING")
            "ABSENT" -> listOf("ABSENT", "UNKNOWN")
            else -> listOf(state)
        }
        val key = overrides.keys.firstOrNull { k ->
            tokens.any { t -> k.contains(t, ignoreCase = true) }
        } ?: return null
        return overrides[key]
    }
}
