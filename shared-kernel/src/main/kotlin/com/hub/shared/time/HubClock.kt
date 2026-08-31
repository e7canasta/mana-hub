package com.hub.shared.time

import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

/**
 * El reloj del Hub, con el mismo vocabulario que el de mana-hive.
 *
 * ── Por que existe ──────────────────────────────────────────
 *
 * mana-hive es disciplinado con el tiempo: sus motores lo declaran en el
 * encabezado ("Now is injected, never Instant.now()") y su reloj se puede poner
 * en manual y adelantar por comando, que es lo que hace el simulador de
 * escenarios. El Hub no tenia reloj: cada estampa era `Instant.now()`.
 *
 * El resultado son **dos lineas de tiempo en la misma base**, y se ve:
 *
 *   scene_events.timestamp      2026-09-03T23:15Z   ← reloj simulado
 *   sentinel_signals.timestamp  2026-09-03T23:15Z   ← reloj simulado
 *   episodes.occurred_at        2026-09-03T23:15Z   ← reloj simulado
 *   history_episode_reviews     2026-08-31T14:15Z   ← reloj real
 *   current_bed_states.state_since  la hora de arranque del Hub
 *
 * Un episodio que ocurre el 3 de septiembre, revisado el 31 de agosto. La
 * secuencia deja de poder leerse, y el panel calcula frescura y "hace N dias"
 * contra dos escalas distintas.
 *
 * No es un problema de zona horaria — eso ya se corrigio aparte. Es que el Hub
 * inventaba el tiempo en vez de compartirlo.
 *
 * ── Que NO resuelve ─────────────────────────────────────────
 *
 * Esto no vuelve determinista al Hub ni lo convierte en un motor puro. Sigue
 * siendo un system of record con IO. Lo unico que hace es que, cuando alguien
 * corre un escenario, **todo lo que el Hub escriba caiga en la misma escala**
 * que lo que hive emite. En produccion el modo es el de sistema y nada cambia.
 */
interface HubClock {
    fun now(): Instant
}

/**
 * Reloj conmutable: sistema por defecto, manual cuando un escenario lo pide.
 *
 * El estado vive en un `AtomicReference` y no en dos campos: el modo y el
 * instante tienen que cambiar juntos o hay una ventana donde el reloj esta en
 * manual sin instante, y ahi devolveria la hora real justo en el medio de una
 * corrida — que es el bug que este archivo existe para evitar.
 */
@Component
class SwitchableHubClock : HubClock {

    private data class State(val manualAt: Instant?)

    private val state = AtomicReference(State(manualAt = null))

    override fun now(): Instant = state.get().manualAt ?: Instant.now()

    /** "A partir de aca, el tiempo es este." */
    fun useManual(startAt: Instant) {
        state.set(State(manualAt = startAt))
    }

    /**
     * Adelanta el tiempo manual.
     *
     * Falla si el reloj esta en modo sistema en vez de adelantar sobre la hora
     * real: adelantar el reloj del mundo no significa nada, y devolver un
     * silencioso no-op dejaria a un escenario creyendo que avanzo.
     */
    fun advance(duration: Duration): Instant {
        val current = state.get().manualAt
            ?: error("El reloj esta en modo sistema: no hay tiempo manual que adelantar")
        val next = current.plus(duration)
        state.set(State(manualAt = next))
        return next
    }

    /** Vuelve al reloj del mundo. */
    fun useSystem() {
        state.set(State(manualAt = null))
    }

    val isManual: Boolean get() = state.get().manualAt != null
}
