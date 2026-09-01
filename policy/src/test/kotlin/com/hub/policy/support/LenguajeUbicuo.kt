package com.hub.policy.support

import com.hub.policy.domain.model.*
import com.hub.shared.domain.ResidentId
import java.time.Instant

/**
 * Lenguaje Ubicuo — Vernon / Fowler para mana-hub/policy.
 *
 * El director médico no habla de `AlarmProfileVersion` ni de `DAG`.
 * Habla de "residente", "riesgo", "nivel de vigilancia", "ajuste".
 *
 * Este DSL es el puente: Kotlin idiomático (Schmid + Spring 2026) que
 * a la vez es legible como una historia clínica.
 *
 * Fowler: "Fluent Interface" + "Ubiquitous Language"
 * Vernon: el Aggregate (Perfil) protege sus invariantes; el DSL es solo la forma de contarlo.
 * Kotlin: @DslMarker + extension + infix + sugar para que la prueba lea como español.
 */

// ── Vocabulario que entiende el director ───────────────────────────────────

object Riesgo {
    const val BAJO = "bajo"
    const val MEDIO = "medio"
    const val ALTO = "alto"
    const val CRITICO = "critico"
}

object Vigilancia {
    const val GENERAL = "Monitoreo General"       // STANDARD
    const val VIGILIA = "Vigilia Nocturna"        // NIGHT_WANDERING
    const val CAIDA = "Riesgo de Caída"           // FALL_RISK
    const val CRITICA = "Crítico"                 // CRITICAL
}

object Ayuda {
    const val NINGUNA = "none"
    const val ANDADOR = "walker"
    const val SILLA = "wheelchair"
}

// ── BDD en español — azúcar sintáctico ─────────────────────────────────────
//
//   dado("un residente con riesgo ALTO") { ... }
//   cuando("consulto su nivel") { ... }
//   entonces("debe ser Riesgo de Caída") { ... }
//
// Son solo alias semánticos; no añaden framework. La ventaja es que la spec
// se lee corrida por un médico y mapea 1:1 al reporte JaCoCo.

inline fun dado(contexto: String, block: () -> Unit) = block()
inline fun cuando(accion: String, block: () -> Unit) = block()
inline fun entonces(esperado: String, block: () -> Unit) = block()
inline fun y(tambien: String, block: () -> Unit) = block()

// ── DSL para construir perfiles — Fowler: "Expression Builder" ─────────────

@DslMarker
annotation class PerfilDsl

@PerfilDsl
class PerfilBuilder {
    var riesgo: RiskLevel = RiskLevel.MEDIUM
    var ayuda: MobilityAid = MobilityAid.NONE
    var autopiloto: Boolean = false
    var modo: PolicyMode = PolicyMode.PRESET
    var plantilla: String? = null
    var residenteId: String = "residente-${System.nanoTime()}"
    var actualizadoPor: String = "dra.garcia"

    fun riesgo(nivel: String) {
        riesgo = RiskLevel.from(nivel)
    }

    fun ayuda(tipo: String) {
        ayuda = MobilityAid.from(tipo)
    }

    fun plantilla(id: String) {
        plantilla = id
    }

    fun build(): AlarmProfileVersion {
        // Creamos y luego mutamos vía update para respetar invariantes del Aggregate (Vernon)
        val base = AlarmProfileVersion.create(ResidentId(residenteId), actualizadoPor)
        return base.update(
            mobilityAid = ayuda,
            autopilot = autopiloto,
            mode = modo,
            templateId = plantilla?.let { TemplateId.from(it) },
            riskLevel = riesgo,
            updatedBy = actualizadoPor,
        )
    }
}

/** Entry-point DSL: `perfil { riesgo("alto"); ayuda("wheelchair") }` */
fun perfil(block: PerfilBuilder.() -> Unit): AlarmProfileVersion =
    PerfilBuilder().apply(block).build()

/** Atajo legible para specs del director: `unResidente conRiesgo "alto"` */
infix fun String.conRiesgo(nivel: String): AlarmProfileVersion {
    val id = this
    return perfil { residenteId = id; riesgo(nivel) }
}

infix fun AlarmProfileVersion.conAyuda(tipo: String): AlarmProfileVersion =
    this.update(
        mobilityAid = MobilityAid.from(tipo),
        autopilot = null, mode = null, templateId = null, riskLevel = null, updatedBy = null
    )

// ── Helpers de catálogo ─────────────────────────────────────────────────────

fun catalogoPara(nivel: WatchLevel) = DagCatalogs.forLevel(nivel)
fun catalogoPara(nivel: String) = DagCatalogs.forLevel(WatchLevel.from(nivel))

// ── Helpers de tiempo / assert ──────────────────────────────────────────────

fun ahora(): Instant = Instant.now()

// Frases para que la spec documente la intención clínica
fun descripcionRegla(estado: StateKind, catalogo: DagCatalog): String {
    val r = catalogo.residentStates[estado] ?: return "sin regla"
    return when {
        !r.alerts -> "$estado: solo observa"
        r.alertAfter != null -> "$estado: avisa a los ${r.alertAfter.toMinutes()} min (${r.severity})"
        else -> "$estado: alerta al entrar"
    }
}
