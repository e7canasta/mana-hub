package com.hub.care.support

import com.hub.care.domain.model.*
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.WingId
import com.hub.shared.domain.BedId
import java.time.Instant

/**
 * Lenguaje Ubicuo — care: la ronda de enfermería y el hallazgo.
 *
 * Directora: "La ronda de ala Norte empezó a las 22:00. La enfermera visitó a María en cama-12,
 * dejó una nota CLINICAL y cerró la tarea."
 *
 * Vernon: Round (Aggregate) + RoundTask (Entidad) + CareNote (Value Object para hallazgo INSIGHT/PATTERN).
 * Fowler: Builder fluido en español.
 */

inline fun dado(ctx: String, block: () -> Unit) = block()
inline fun cuando(accion: String, block: () -> Unit) = block()
inline fun entonces(esperado: String, block: () -> Unit) = block()

@DslMarker annotation class RondaDsl

@RondaDsl
class RondaBuilder {
    var ala: String = "ala-norte"
    var programada: Instant? = Instant.parse("2026-09-01T22:00:00Z")
    fun build(): Round = Round.create(WingId(ala), programada)
}

fun ronda(block: RondaBuilder.() -> Unit) = RondaBuilder().apply(block).build()

fun tareaDeRonda(rondaId: RoundId, residente: String = "maria-1", cama: String? = "cama-12") =
    RoundTask.create(rondaId, ResidentId(residente), cama?.let { BedId(it) })

fun notaClinica(residente: String = "maria-1", autor: String = "enfermera.ana", tipo: CareNoteKind = CareNoteKind.CLINICAL, texto: String = "Hallazgo: patrón de salidas") =
    CareNote.create(ResidentId(residente), autor, tipo, texto, durationMin = 5)

fun notaDe(hallazgo: String) = CareNote.create(ResidentId("maria-1"), "ml-model", CareNoteKind.INSIGHT, hallazgo, 10)
