package com.hub.observation.support

import com.hub.observation.domain.model.*
import com.hub.shared.domain.BedId
import com.hub.shared.domain.Identifier
import com.hub.shared.domain.ResidentId
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Lenguaje Ubicuo — observation: la memoria de lo que la cámara vio.
 *
 * Director médico: "María se sentó al borde a las 3:12, estuvo 2 min, volvió a acostarse"
 * Enfermería: "¿Hay alguien en la cama 12? ¿Entró staff?"
 *
 * Este DSL traduce esos verbos a eventos de dominio sin exponer
 * `POST /internal/v1/events` ni `scene-events`.
 *
 * Vernon: SensorEvent / SceneEvent / CurrentBedState son Aggregates/Eventos.
 * Fowler: Expression Builder + Object Mother + Sugar Kotlin.
 */

// ── BDD azúcar — mismo que policy, ubicuo ───────────────────────────────────
inline fun dado(ctx: String, block: () -> Unit) = block()
inline fun cuando(accion: String, block: () -> Unit) = block()
inline fun entonces(esperado: String, block: () -> Unit) = block()
inline fun y(tambien: String, block: () -> Unit) = block()

// ── Vocabulario clínico ──────────────────────────────────────────────────────
object EstadoCama {
    const val VACIA = "empty"
    const val OCUPADA = "occupied"
    const val DURMIENDO = "sleeping"
    const val BORDE = "bed_edge"
    const val DEAMBULANDO = "wandering"
    const val BANO = "bathroom"
}

object CategoriaAviso {
    const val INFO = "INFORMATIONAL"
    const val ALERTA = "WARNING"
    const val CRITICO = "CRITICAL"
}

// ── Builders fluentes ───────────────────────────────────────────────────────

@DslMarker annotation class ObservacionDsl

@ObservacionDsl
class PercepcionBuilder {
    var camaId: String = "cama-12"
    var residenteId: String? = "res-001"
    var tipo: SensorEventKind = SensorEventKind.CAMERA
    var estado: String? = "occupied"
    var habitacion: String? = "room-1"
    var durmiendo: Boolean? = false
    var cuando: Instant = Instant.parse("2026-09-01T03:12:00Z")
    var eventoId: String = "evt-${System.nanoTime()}"
    var monitor: String = "cam-1"

    fun build(): Triple<SensorEvent, CurrentBedState, Instant> {
        val bedId = BedId(camaId)
        val rid = residenteId?.let { ResidentId(it) }
        val sensor = SensorEvent.create(
            sourceEventId = eventoId, monitorKey = monitor, bedId = bedId, residentId = rid,
            kind = tipo, roomState = habitacion, state = estado, sleeping = durmiendo, occurredAt = cuando
        )
        val bedState = CurrentBedState(
            bedId = bedId, residentId = rid, roomState = habitacion, state = estado, substate = null,
            sleeping = durmiendo, stateSince = cuando, updated = cuando, source = tipo.name, sourceEventId = eventoId
        )
        return Triple(sensor, bedState, cuando)
    }
}

fun percepcion(block: PercepcionBuilder.() -> Unit) = PercepcionBuilder().apply(block).build()

@ObservacionDsl
class EscenaBuilder {
    var camaId: String = "cama-12"
    var residenteId: String? = "res-001"
    var tipo: SceneEventType = SceneEventType.STATE_CHANGED
    var desde: SceneState? = SceneState.SLEEPING
    var hacia: SceneState? = SceneState.WANDERING
    var trigger: TriggerType? = TriggerType.EVENT_DRIVEN
    var cuando: Instant = Instant.parse("2026-09-01T03:13:00Z")
    var eventoId: String = "scene-${System.nanoTime()}"

    fun build(): SceneEvent = SceneEvent(
        id = Identifier.random(), eventId = eventoId, bedId = BedId(camaId),
        residentId = residenteId?.let { ResidentId(it) }, eventType = tipo,
        fromState = desde, toState = hacia, triggerType = trigger,
        timestamp = cuando, payloadJson = "{}", twinSnapshotJson = "{}"
    )
}

fun escena(block: EscenaBuilder.() -> Unit) = EscenaBuilder().apply(block).build()

fun notificacion(
    categoria: String = "INFORMATIONAL",
    camaId: String? = "cama-12",
    residenteId: String? = "res-001",
    tipo: String = "bed_edge_detected",
    cuando: Instant = Instant.parse("2026-09-01T03:12:30Z"),
) = NotificationEvent.create(
    category = NotificationCategory.from(categoria),
    bedId = camaId, residentId = residenteId,
    eventType = tipo, timestamp = cuando, ruleId = "rule-1", riskLevel = "medium", payloadJson = "{}"
)

// ── Resúmenes clínicos ───────────────────────────────────────────────────────

fun resumenSueno(
    residente: String = "res-001", dia: LocalDate = LocalDate.of(2026, 9, 1),
    calma: Int = 240, inquieto: Int = 60, despierto: Int = 30, fueraCama: Int = 12, salidas: Int = 2,
) = SleepSummary.create(
    sourceRecordId = "sleep-$dia", residentId = ResidentId(residente), observedOn = dia,
    calmMinutes = calma, restlessMinutes = inquieto, awakeMinutes = despierto,
    outOfBedMinutes = fueraCama, bedExitCount = salidas, wakeCount = 1,
    source = "model", modelVersion = "2.1.0", confidence = 0.9,
    startedAt = LocalDateTime.of(2026, 9, 1, 22, 0), endedAt = LocalDateTime.of(2026, 9, 2, 6, 0)
)

fun resumenMovilidad(
    residente: String = "res-001", dia: LocalDate = LocalDate.of(2026, 9, 1),
) = MobilitySummary.create(
    sourceRecordId = "mob-$dia", residentId = ResidentId(residente), observedOn = dia,
    inBedMinutes = 480, outOfBedMinutes = 90, outOfSightMinutes = 5,
    walkingMinutes = 30, distanceMeters = 120.0, transferCount = 3,
    source = "model", modelVersion = "2.1.0", confidence = 0.9
)

fun resumenBano(
    residente: String = "res-001", dia: LocalDate = LocalDate.of(2026, 9, 1),
) = BathroomSummary.create(
    sourceRecordId = "bath-$dia", residentId = ResidentId(residente), observedOn = dia,
    visitCount = 4, nightVisitCount = 2, assistedCount = 1, totalMinutes = 18,
    source = "model", modelVersion = "2.1.0", confidence = 0.9
)

// ── Helpers de aserción legible ─────────────────────────────────────────────
fun camaOcupada(state: CurrentBedState) = state.state == "occupied" || state.roomState == "occupied"
fun escenaEsTransicion(e: SceneEvent) = e.eventType == SceneEventType.STATE_CHANGED && e.fromState != e.toState
