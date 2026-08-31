package com.hub.clients.panel

import com.hub.shared.panel.*
import com.manahive.contracts.policy.MobilityAid
import com.manahive.contracts.policy.PolicyMode
import com.manahive.contracts.policy.RiskLevel

/**
 * Panel SDK - Ejemplos por caso de uso.
 *
 * Cada escenario es una funcion independiente que demuestra
 * la secuencia de llamadas completa para un flujo real.
 *
 * Correr: ./gradlew :clients:compileKotlin && java -cp "..." com.hub.clients.panel.PanelExamplesKt
 */
private const val BASE = "http://localhost:8081"

fun main() {
    println("========================================")
    println("  Panel SDK - Casos de Uso")
    println("========================================\n")

    escenario1_residenteRail()
    escenario2_residenteDetalle()
    escenario3_episodioFeed()
    escenario4_episodioDetalleCompleto()
    escenario5_revisionEpisodio()
    escenario6_notaEpisodio()
    escenario7_preferenciasLista()
    escenario8_preferenciasDetalle()
    escenario9_catalogo()
    escenario10_guardarPreferencias()
}

// ══════════════════════════════════════════════════════════════
//  ESCENARIO 1: Rail de residentes
//  Caso de uso: Panel carga y muestra la grilla de residentes
// ══════════════════════════════════════════════════════════════
fun escenario1_residenteRail() {
    println("--- Escenario 1: Rail de residentes ---")
    println("  Flujo: GET /panel/residents -> renderizar grilla\n")

    panel(BASE) {
        val residents = residents.list()
        println("  Total: ${residents.size} residentes activos\n")
        residents.forEach { r ->
            val wing = r.location?.wingName ?: "sin asignar"
            val room = r.location?.roomNumber ?: "-"
            val state = r.currentState?.state ?: "desconocido"
            val staff = if (r.currentState?.staffPresent == true) " [personal presente]" else ""
            println("  ${r.fullName.padEnd(20)} $wing/${room}  -> $state$staff")
        }
    }
    println()
}

// ══════════════════════════════════════════════════════════════
//  ESCENARIO 2: Detalle de residente
//  Caso de uso: Click en residente para ver su ficha
// ══════════════════════════════════════════════════════════════
fun escenario2_residenteDetalle() {
    println("--- Escenario 2: Detalle de residente ---")
    println("  Flujo: GET /panel/residents -> GET /panel/residents/{id} -> GET /panel/residents/{id}/notes\n")

    panel(BASE) {
        val all = residents.list()
        val target = all.first()

        val detail = residents.detail(target.id)
        println("  Residente: ${detail.fullName}")
        println("  Ubicacion: ${detail.location?.wingName}/${detail.location?.roomNumber} cama ${detail.location?.bedLabel}")
        println("  Estado:    ${detail.currentState?.state ?: "desconocido"}")
        println("  Desde:     ${detail.currentState?.stateSince ?: '-'}")

        val notes = residents.notes(target.id)
        println("  Notas:     ${notes.notes.size}")
        notes.notes.forEach { n ->
            println("    [${n.kind}] ${n.body}")
        }
    }
    println()
}

// ══════════════════════════════════════════════════════════════
//  ESCENARIO 3: Feed de episodios
//  Caso de uso: Panel carga tabla de episodios con resumen
// ══════════════════════════════════════════════════════════════
fun escenario3_episodioFeed() {
    println("--- Escenario 3: Feed de episodios ---")
    println("  Flujo: GET /panel/episodes -> renderizar tabla + resumen\n")

    panel(BASE) {
        val feed = episodes.feed()
        println("  Resumen: ${feed.summary.total} total | ${feed.summary.pending} pendientes | ${feed.summary.injured} con lesion\n")

        feed.episodes.forEach { ep ->
            val injury = if (ep.injury != null) " [lesion: ${ep.injury}]" else ""
            val verdict = ep.verdict?.let { " [${it.value}]" } ?: " [sin revision]"
            println("  ${ep.severity.value.padEnd(10)} ${ep.residentName.padEnd(20)} ${ep.title}$injury$verdict")
        }
    }
    println()
}

// ══════════════════════════════════════════════════════════════
//  ESCENARIO 4: Detalle completo de episodio
//  Caso de uso: Click en episodio para ver narrativa + timeline
// ══════════════════════════════════════════════════════════════
fun escenario4_episodioDetalleCompleto() {
    println("--- Escenario 4: Detalle de episodio ---")
    println("  Flujo: GET /panel/episodes/{id} + GET /episodes/{id}/notes + GET /episodes/{id}/interventions\n")

    panel(BASE) {
        val feed = episodes.feed()
        val target = feed.episodes.first()

        val detail = episodes.detail(target.id)
        println("  Episodio:  ${detail.id}")
        println("  Residente: ${detail.residentName}")
        println("  Severidad: ${detail.severity.value}")
        println("  Tipo:      ${detail.kind}")
        println("  Titulo:    ${detail.title}")
        println("  Ocurrio:   ${detail.occurredAt}")
        println("  Status:    ${detail.status.value}")
        detail.narrative?.let { println("  Narrativa: $it") }
        detail.injury?.let { println("  Lesion:    $it") }
        detail.selfRecovery?.let { println("  Auto-rec:  $it") }
        detail.responseSeconds?.let { println("  Respuesta: ${it}s") }
        detail.escalationLevel?.let { println("  Escalacion: nivel $it") }

        println("\n  Timeline: ${detail.timeline.size} eventos")
        detail.timeline.forEach { t ->
            println("    ${t.at} [${t.type}] ${t.description ?: t.eventType ?: t.signalType ?: '-'}")
        }

        println("\n  Notas del episodio:")
        val notes = episodes.notes(target.id)
        if (notes.notes.isEmpty()) println("    (ninguna)")
        notes.notes.forEach { n ->
            println("    [${n.kind}] ${n.authorName}: ${n.body}")
        }

        println("\n  Intervenciones:")
        val interventions = episodes.interventions(target.id)
        if (interventions.interventions.isEmpty()) println("    (ninguna)")
        interventions.interventions.forEach { iv ->
            println("    ${iv.kind} por ${iv.performedBy} - ${iv.detail}")
        }
    }
    println()
}

// ══════════════════════════════════════════════════════════════
//  ESCENARIO 5: Revision de episodio
//  Caso de uso: Enfermera revisa episodio y asigna veredicto
// ══════════════════════════════════════════════════════════════
fun escenario5_revisionEpisodio() {
    println("--- Escenario 5: Revision de episodio ---")
    println("  Flujo: GET /panel/episodes -> POST /episodes/{id}/review -> GET /panel/episodes (verificar cambio)\n")

    panel(BASE) {
        val feed = episodes.feed()
        val pending = feed.episodes.firstOrNull { it.verdict == null }

        if (pending == null) {
            println("  No hay episodios pendientes de revision")
            return@panel
        }

        println("  Episodio a revisar: ${pending.id}")
        println("  Residente: ${pending.residentName}")
        println("  Titulo: ${pending.title}")

        val review = episodes.review(
            episodeId = pending.id,
            verdict = EpisodeVerdict.CONFIRMED,
            note = "Confirmado por enfermera de guardia. Sin lesion aparente.",
            actorId = "enfermera_ana"
        )
        println("\n  Revision registrada:")
        println("    Veredicto: ${review.verdict.value}")
        println("    Nota: ${review.reviewNote}")
        println("    Por: ${review.reviewedBy}")
        println("    En: ${review.reviewedAt}")
    }
    println()
}

// ══════════════════════════════════════════════════════════════
//  ESCENARIO 6: Agregar nota a episodio
//  Caso de uso: Enfermera documenta hallazgo clinico
// ══════════════════════════════════════════════════════════════
fun escenario6_notaEpisodio() {
    println("--- Escenario 6: Nota en episodio ---")
    println("  Flujo: GET /panel/episodes -> POST /episodes/{id}/notes\n")

    panel(BASE) {
        val feed = episodes.feed()
        val target = feed.episodes.first()

        val note = episodes.createNote(
            episodeId = target.id,
            kind = NoteKind.CLINICAL_NOTE,
            body = "Residente orientado, sin signos de trauma craneal. Signos vitales estables.",
            authorId = "doctor_lopez"
        )
        println("  Nota creada en episodio ${target.id}:")
        println("    ID: ${note.id}")
        println("    Tipo: ${note.kind}")
        println("    Autor: ${note.authorId}")
        println("    Texto: ${note.body}")
    }
    println()
}

// ══════════════════════════════════════════════════════════════
//  ESCENARIO 7: Lista de preferencias
//  Caso de uso: Panel muestra tabla de configuracion de alarmas
// ══════════════════════════════════════════════════════════════
fun escenario7_preferenciasLista() {
    println("--- Escenario 7: Lista de preferencias ---")
    println("  Flujo: GET /panel/preferences -> renderizar tabla\n")

    panel(BASE) {
        val prefs = preferences.list()
        println("  Total: ${prefs.size} perfiles\n")
        prefs.forEach { p ->
            val wing = p.location?.wingName ?: "sin cama"
            println("  ${p.residentName.padEnd(20)} ${wing.padEnd(22)} risk=${p.riskLevel.name.padEnd(7)} aid=${p.mobilityAid.name.padEnd(10)} mode=${p.mode.name}  autopilot=${p.autopilot}")
        }
    }
    println()
}

// ══════════════════════════════════════════════════════════════
//  ESCENARIO 8: Detalle de preferencias
//  Caso de uso: Click en residente para ver su perfil de alarmas
// ══════════════════════════════════════════════════════════════
fun escenario8_preferenciasDetalle() {
    println("--- Escenario 8: Detalle de preferencias ---")
    println("  Flujo: GET /panel/preferences/{id}\n")

    panel(BASE) {
        val prefs = preferences.list()
        val target = prefs.first { it.riskLevel == RiskLevel.HIGH }

        val detail = preferences.detail(target.residentId)
        println("  Residente: ${detail.residentName}")
        println("  Ubicacion: ${detail.location?.wingName}/${detail.location?.roomNumber}")
        println("  Risk:      ${detail.riskLevel.name}")
        println("  Movilidad: ${detail.mobilityAid.name}")
        println("  Modo:      ${detail.mode.name}")
        println("  Autopilot: ${detail.autopilot}")
        println("  Template:  ${detail.templateId ?: "ninguno"}")
        println("  Actualizado por: ${detail.updatedBy ?: '-'}")
        println("  En: ${detail.updatedAt ?: '-'}")
        detail.recommendation?.let { r ->
            println("  Recomendacion: nivel=${r.level.name} score=${r.score} factores=${r.factors}")
        }
    }
    println()
}

// ══════════════════════════════════════════════════════════════
//  ESCENARIO 9: Catalogo de preferencias
//  Caso de uso: Panel carga catálogo para formulario de config
// ══════════════════════════════════════════════════════════════
fun escenario9_catalogo() {
    println("--- Escenario 9: Catalogo de preferencias ---")
    println("  Flujo: GET /panel/preferences/catalog -> renderizar form\n")

    panel(BASE) {
        val cat = preferences.catalog()
        println("  Niveles:    ${cat.levels.joinToString { it.name }}")
        println("  Movilidad:  ${cat.mobilityAids.joinToString { it.name }}")
        println("  Factores:   ${cat.riskFactors.joinToString { "${it.label} (${it.icon})" }}")
        println("\n  Transiciones (${cat.transitions.size}):")
        cat.transitions.forEach { t ->
            val req = t.requiresAid?.let { " [requiere ${it.name}]" } ?: ""
            val lock = if (t.locked) " [siempre activa]" else ""
            println("    ${t.id.value.padEnd(18)} ${t.label.padEnd(35)} grupo=${t.group.value}$req$lock")
            if (t.params.isNotEmpty()) {
                t.params.forEach { p ->
                    println("      param: ${p.label} (${p.min}-${p.max} ${p.unit ?: ""})")
                }
            }
        }
        println("\n  Presets por nivel:")
        cat.presets.forEach { (level, shifts) ->
            println("    $level:")
            shifts.forEach { (trans, shift) ->
                println("      ${trans.value.padEnd(18)} dia=${shift.day.value} noche=${shift.night.value}")
            }
        }
    }
    println()
}

// ══════════════════════════════════════════════════════════════
//  ESCENARIO 10: Guardar preferencias
//  Caso de uso: Enfermera cambia nivel de riesgo de residente
// ══════════════════════════════════════════════════════════════
fun escenario10_guardarPreferencias() {
    println("--- Escenario 10: Guardar preferencias ---")
    println("  Flujo: GET /panel/preferences/{id} -> POST /preferences/{id}/save -> GET (verificar)\n")

    panel(BASE) {
        val prefs = preferences.list()
        val target = prefs.first()

        println("  ANTES:")
        println("    ${target.residentName}: risk=${target.riskLevel.name} aid=${target.mobilityAid.name} mode=${target.mode.name}")

        val saved = preferences.save(target.residentId) {
            riskLevel = RiskLevel.HIGH
            mobilityAid = MobilityAid.WALKER
            reason = "Tras 3 caidas en 7 dias. Personal medico solicita upgrade."
            updatedBy = "doctor_lopez"
        }

        println("\n  GUARDADO:")
        println("    risk=${saved.riskLevel.name} aid=${saved.mobilityAid.name} autopilot=${saved.autopilot}")
        println("    version guardada en: ${saved.updatedAt}")
    }

    println("\n  VERIFICACION:")
    panel(BASE) {
        val verify = preferences.detail("jose")
        println("    ${verify.residentName}: risk=${verify.riskLevel.name} aid=${verify.mobilityAid.name}")
    }
    println()
}
