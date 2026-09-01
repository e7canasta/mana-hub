package com.hub.history.application.service

import com.hub.history.domain.model.timeline.EpisodeTimelineEvent
import com.hub.history.domain.model.timeline.EpisodeTimelineEventId
import com.hub.history.domain.model.timeline.EventType
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.port.ObservationQueryPort
import com.hub.shared.domain.port.SceneEventSnapshot
import com.hub.shared.domain.port.SentinelSignalSnapshot
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

/**
 * Deriva la linea de tiempo de un episodio de lo que ya esta guardado.
 *
 * [EpisodeTimelineBuilder] construye el timeline **cuando el evento llega por el
 * webhook del bridge**. Los episodios que produce el motor entran directo a
 * `sentinel_signals` y `scene_events` sin pasar por ahi, asi que no tienen una
 * sola fila en `episode_timeline_events` — y `GET /history-episodes/{id}/timeline`
 * respondia 404 para justamente los episodios que el panel necesita mostrar.
 *
 * Un 404 ahi no dice "este episodio no tuvo historia": dice "no se por donde
 * entro". Y la historia existe, repartida en dos tablas.
 *
 * Se **deriva al leer** en vez de rellenar la tabla: no necesita migracion ni un
 * backfill que alguien tenga que recordar correr despues de cada simulacion, es
 * idempotente por construccion, y cuando el webhook si corrio esas filas ganan
 * — la regla esta en [EpisodeTimelineService] y en un solo lugar.
 *
 * ── Por que entidades y no SQL ──────────────────────────────
 *
 * La primera version consultaba con `JdbcTemplate` y SQL a mano. Los dos unicos
 * bugs que tuvo salieron **exactamente de ahi**: las columnas `timestamp` son
 * sin zona, `rs.getTimestamp().toInstant()` las interpretaba en la zona del JVM
 * y `Timestamp.from(instant)` ligaba el parametro igual de mal — tres horas de
 * corrimiento al leer, y una ventana corrida que no devolvia ninguna fila de
 * escena. Hibernate ya mapea esas columnas a `Instant` y no tiene el problema,
 * que es por que el endpoint de admin siempre dio bien la hora.
 *
 * Yendo por los repositorios JPA la clase entera de bug desaparece, y de paso el
 * codigo dice que busca en vez de como. El filtro de ventana se hace en memoria
 * a proposito: son los eventos de una cama en una noche, y la claridad vale mas
 * que una consulta afinada para un volumen que no tenemos.
 */
@Service
class EpisodeTimelineDeriver(
    private val observationPort: ObservationQueryPort,

    /* El contexto alrededor del episodio, configurable.
     *
     * Media hora es lo que un turno noche considera "recien": mas atras —o mas
     * adelante— el contexto deja de explicar el episodio y empieza a ser otra
     * cosa. Pero es un juicio clinico, no una constante tecnica, asi que se
     * configura: una residencia con rondas cada dos horas puede querer otra
     * cosa, y no deberia tener que recompilar para decirlo. */
    @Value("\${mana.timeline.lookback:PT30M}")
    private val lookback: Duration = Duration.ofMinutes(30),

    @Value("\${mana.timeline.lookahead:PT15M}")
    private val lookahead: Duration = Duration.ofMinutes(15),
) {

    /** El unico estado seguro del dominio: `RiskGroup.SAFE` es `Lying` y nada mas. */
    private val safeState = "Lying"

    fun derive(episodeId: String): List<EpisodeTimelineEvent> {
        val signalRows = observationPort.findSignalsByEpisodeId(episodeId)
            .sortedBy { it.observedAt }
        if (signalRows.isEmpty()) return emptyList()

        val residentId = signalRows.firstOrNull { it.residentId != null }?.residentId
            ?: "unknown"
        val bedId = signalRows.first().bedId
        val opened = signalRows.first().observedAt
        val closed = signalRows.last().observedAt

        val onBed = observationPort.findScenesByBedId(bedId).sortedBy { it.observedAt }
        val from = windowStart(onBed, opened)
        val to = windowEnd(onBed, closed)

        val events = mutableListOf<EpisodeTimelineEvent>()

        signalRows.forEachIndexed { index, s ->
            val type = mapSignal(s) ?: return@forEachIndexed
            events += EpisodeTimelineEvent(
                id = EpisodeTimelineEventId.from("derived-sig-$episodeId-$index"),
                episodeId = episodeId,
                residentId = ResidentId(residentId),
                at = s.observedAt,
                type = type,
                fromState = null,
                toState = s.state,
                description = describeSignal(s),
            )
        }

        onBed
            .filter { it.observedAt >= from && it.observedAt <= to }
            .filter { it.eventType == "TransitionDetected" }
            .forEachIndexed { index, c ->
                events += EpisodeTimelineEvent(
                    id = EpisodeTimelineEventId.from("derived-scene-$episodeId-$index"),
                    episodeId = episodeId,
                    residentId = ResidentId(residentId),
                    at = c.observedAt,
                    type = EventType.UMBRELLA,
                    fromState = c.fromState,
                    toState = c.toState,
                    description = describeScene(c),
                )
            }

        return events.sortedWith(compareBy({ it.at }, { rank(it) }))
    }

    private fun rank(e: EpisodeTimelineEvent): Int = when (e.type) {
        EventType.UMBRELLA -> if (e.fromState != null) 0 else 1
        EventType.OPENED, EventType.ESCALATED, EventType.NOTIFIED -> 2
        EventType.RESPONDED, EventType.STAFF_ARRIVED -> 3
        EventType.RECOVERY, EventType.CLOSED -> 4
    }

    private fun windowStart(onBed: List<SceneEventSnapshot>, openedAt: Instant): Instant {
        val floor = openedAt.minus(lookback)
        return onBed
            .lastOrNull { it.toState == safeState && it.observedAt <= openedAt && it.observedAt >= floor }
            ?.observedAt ?: floor
    }

    private fun windowEnd(onBed: List<SceneEventSnapshot>, closedAt: Instant): Instant {
        val ceiling = closedAt.plus(lookahead)
        return onBed
            .firstOrNull { it.toState == safeState && it.observedAt >= closedAt && it.observedAt <= ceiling }
            ?.observedAt ?: ceiling
    }

    private fun mapSignal(s: SentinelSignalSnapshot): EventType? = when (s.signalType) {
        "EPISODE_OPENED" -> EventType.OPENED
        "EPISODE_ESCALATED", "SEVERITY_RAMP" -> EventType.ESCALATED
        "NOTICE_DISPATCHED", "ALARM_DISPATCHED" -> EventType.NOTIFIED
        "NOTICE_RESOLVED", "STAFF_RESPONDED" -> EventType.RESPONDED
        "STAFF_ARRIVED" -> EventType.STAFF_ARRIVED
        "EPISODE_CLOSED" -> if (s.cause == "AUTO_RECOVERY") EventType.RECOVERY else EventType.CLOSED
        "UMBRELLA_EVENT" -> EventType.UMBRELLA
        else -> null
    }

    private fun describeSignal(s: SentinelSignalSnapshot): String = signalDescriptions[s.signalType]?.invoke(s)
        ?: s.signalType ?: "UNKNOWN"

    private fun describeScene(c: SceneEventSnapshot): String = sceneDescriptions[c.eventType]?.invoke(c)
        ?: c.eventType ?: "UNKNOWN"

    private val signalDescriptions: Map<String, (SentinelSignalSnapshot) -> String> = mapOf(
        "EPISODE_OPENED" to { s -> "Se abrió el episodio" + (s.trigger?.let { " por ${humanState(it)}" } ?: "") },
        "EPISODE_ESCALATED" to { s -> "Subió de severidad" + (s.severity?.let { " a ${it.lowercase()}" } ?: "") },
        "SEVERITY_RAMP" to { s -> "Subió de severidad" + (s.severity?.let { " a ${it.lowercase()}" } ?: "") },
        "UMBRELLA_EVENT" to { s ->
            if (s.triggerOn == "COME_BACK")
                "Venció el plazo de retorno" + (s.state?.let { " a ${humanState(it)}" } ?: "")
            else
                "Movimiento dentro del episodio" + (s.state?.let { " (${humanState(it)})" } ?: "")
        },
        "EPISODE_CLOSED" to { s -> when (s.cause) {
            "AUTO_RECOVERY" -> "Volvió sola a una posición segura y el episodio cerró"
            "STAFF", "STAFF_ASSIST" -> "Cerró cuando llegó el personal"
            else -> "El episodio cerró" + (s.cause?.let { " ($it)" } ?: "")
        }},
    )

    private val sceneDescriptions: Map<String, (SceneEventSnapshot) -> String> = mapOf(
        "TransitionDetected" to { c -> "${humanState(c.fromState)} → ${humanState(c.toState)}" },
        "ComeBackExceeded" to { _ -> "No volvió al estado de referencia dentro del plazo" },
        "DwellExceeded" to { _ -> "Se quedó más tiempo del tolerado" },
    )

    private val stateLabels: Map<String, String> = mapOf(
        "LYING" to "acostado",
        "SITTINGINBED" to "incorporado en la cama", "SITTING_IN_BED" to "incorporado en la cama",
        "ATTEMPTINGEXIT" to "intentando salir", "ATTEMPTING_EXIT" to "intentando salir",
        "BEDEDGE" to "al borde de la cama", "BED_EDGE" to "al borde de la cama",
        "STANDING" to "de pie",
        "ONFLOOR" to "en el piso", "ON_FLOOR" to "en el piso",
        "INBATHROOM" to "en el baño", "IN_BATHROOM" to "en el baño",
        "INROOM" to "en la habitación", "IN_ROOM" to "en la habitación",
        "INHALLWAY" to "en el pasillo", "IN_HALLWAY" to "en el pasillo",
        "OUTDOOR" to "afuera",
        "ABSENT" to "fuera de la habitación",
        "INCHAIR" to "en una silla", "IN_CHAIR" to "en una silla",
        "INWHEELCHAIR" to "en la silla de ruedas", "IN_WHEELCHAIR" to "en la silla de ruedas",
        "UNKNOWN" to "sin determinar",
    )

    private fun humanState(raw: String?): String =
        raw?.uppercase()?.let { stateLabels[it] } ?: raw?.lowercase() ?: "sin determinar"
}
