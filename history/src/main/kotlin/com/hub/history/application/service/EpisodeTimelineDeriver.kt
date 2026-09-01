package com.hub.history.application.service

import com.hub.history.domain.model.timeline.EpisodeTimelineEvent
import com.hub.history.domain.model.timeline.EpisodeTimelineEventId
import com.hub.history.domain.model.timeline.EventType
import com.hub.observation.domain.model.SceneEvent
import com.hub.observation.domain.model.SentinelSignal
import com.hub.observation.domain.repository.SceneEventRepository
import com.hub.observation.domain.repository.SentinelSignalRepository
import com.hub.shared.domain.BedId
import com.hub.shared.domain.ResidentId
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
    private val signals: SentinelSignalRepository,
    private val scenes: SceneEventRepository,

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
        val signalRows = signals.findByEpisodeId(episodeId).sortedBy { it.timestamp }
        if (signalRows.isEmpty()) return emptyList()

        val residentId = signalRows.firstOrNull { it.residentId != null }?.residentId
            ?: ResidentId("unknown")
        val bedId = signalRows.first().bedId.value
        val opened = signalRows.first().timestamp
        val closed = signalRows.last().timestamp

        /* Todos los eventos de la cama, y la ventana se recorta acá.
         *
         * Los eventos de escena no llevan `episode_id`: son del episodio por
         * **cuando pasaron**. Lo que hace sana esa atribucion es una premisa del
         * dominio — no hay mas de un episodio abierto por residente a la vez, asi
         * que un instante pertenece a lo sumo a uno. El dia que eso deje de
         * valer, esta funcion empieza a repartir eventos entre dos episodios y
         * hay que volver aca. */
        val onBed = scenes.findByBedId(BedId(bedId)).sortedBy { it.timestamp }
        val from = windowStart(onBed, opened)
        val to = windowEnd(onBed, closed)

        val events = mutableListOf<EpisodeTimelineEvent>()

        signalRows.forEachIndexed { index, s ->
            val type = mapSignal(s) ?: return@forEachIndexed
            events += EpisodeTimelineEvent(
                id = EpisodeTimelineEventId.from("derived-sig-$episodeId-$index"),
                episodeId = episodeId,
                residentId = residentId,
                at = s.timestamp,
                type = type,
                fromState = null,
                toState = s.state,
                description = describe(s),
            )
        }

        onBed
            .filter { it.timestamp >= from && it.timestamp <= to }
            /* Solo las transiciones. `ComeBackExceeded` y `DwellExceeded` son el
             * disparador interno del motor, y la senal que producen ya narra el
             * mismo hecho. Emitir los dos contaba una cosa dos veces con palabras
             * distintas, que en una secuencia se lee como si hubieran pasado dos. */
            .filter { it.eventType?.name == "TransitionDetected" }
            .forEachIndexed { index, c ->
                events += EpisodeTimelineEvent(
                    id = EpisodeTimelineEventId.from("derived-scene-$episodeId-$index"),
                    episodeId = episodeId,
                    residentId = residentId,
                    at = c.timestamp,
                    type = EventType.UMBRELLA,
                    fromState = c.fromState?.name,
                    toState = c.toState?.name,
                    description = describe(c),
                )
            }

        /* Dentro del mismo instante, causa antes que consecuencia.
         *
         * Las dos fuentes escriben la misma hora: la persona se movio y el
         * sistema decidio, y el orden de insercion no significa nada. Sin
         * desempate el cierre aparecia **antes** del movimiento que lo provoco, y
         * una secuencia que se contradice a si misma es peor que no tenerla. */
        return events.sortedWith(compareBy({ it.at }, { rank(it) }))
    }

    private fun rank(e: EpisodeTimelineEvent): Int = when (e.type) {
        EventType.UMBRELLA -> if (e.fromState != null) 0 else 1
        EventType.OPENED, EventType.ESCALATED, EventType.NOTIFIED -> 2
        EventType.RESPONDED, EventType.STAFF_ARRIVED -> 3
        EventType.RECOVERY, EventType.CLOSED -> 4
    }

    /**
     * El comienzo del relato, que no es el comienzo del episodio.
     *
     * El episodio se abre cuando el sentinel lo declara, pero para entender que
     * paso hay que ver de donde venia: estaba acostada, se incorporo, se quedo al
     * borde. Sin eso el relato arranca en la consecuencia —el aviso— y el
     * director no puede juzgar si el sistema reacciono a tiempo.
     *
     * Se retrocede hasta la ultima vez que estuvo en un estado seguro. Si no hay
     * ninguno dentro del tope se corta ahi: sin tope, un residente que no vuelve
     * a la cama en toda la noche arrastraria el turno entero adentro del episodio.
     */
    private fun windowStart(onBed: List<SceneEvent>, openedAt: Instant): Instant {
        val floor = openedAt.minus(lookback)
        return onBed
            .lastOrNull { it.toState?.name == safeState && it.timestamp <= openedAt && it.timestamp >= floor }
            ?.timestamp
            ?: floor
    }

    /**
     * El final del relato, que tampoco es el cierre del episodio.
     *
     * Simetrico a [windowStart] y por la misma razon: lo que paso **despues** es
     * parte de la lectura — si volvio a la cama y se quedo, o si se volvio a
     * levantar a los dos minutos. Un episodio que cierra por recuperacion y otro
     * que cierra igual pero reabre enseguida son dos historias distintas, y sin
     * el despues se leen iguales.
     *
     * El tope es mas corto que el de entrada a proposito: hacia atras se busca la
     * causa, que puede estar lejos; hacia adelante el desenlace, que si tarda
     * mucho ya es otro episodio.
     */
    private fun windowEnd(onBed: List<SceneEvent>, closedAt: Instant): Instant {
        val ceiling = closedAt.plus(lookahead)
        return onBed
            .firstOrNull { it.toState?.name == safeState && it.timestamp >= closedAt && it.timestamp <= ceiling }
            ?.timestamp
            ?: ceiling
    }

    /* El mismo vocabulario que `EpisodeTimelineBuilder.mapSentinelEventType`,
     * pero sobre los nombres que usa el motor al persistir la senal
     * (`EPISODE_OPENED`) y no los del webhook (`EpisodeOpened`). Son dos
     * escrituras del mismo hecho y por ahora conviven; unificarlas es del lado
     * del motor, no de aca. */
    private fun mapSignal(s: SentinelSignal): EventType? = when (s.type?.name) {
        "EPISODE_OPENED" -> EventType.OPENED
        "EPISODE_ESCALATED", "SEVERITY_RAMP" -> EventType.ESCALATED
        "NOTICE_DISPATCHED", "ALARM_DISPATCHED" -> EventType.NOTIFIED
        "NOTICE_RESOLVED", "STAFF_RESPONDED" -> EventType.RESPONDED
        "STAFF_ARRIVED" -> EventType.STAFF_ARRIVED
        "EPISODE_CLOSED" -> if (s.cause == "AUTO_RECOVERY") EventType.RECOVERY else EventType.CLOSED
        "UMBRELLA_EVENT" -> EventType.UMBRELLA
        else -> null
    }

    /* Las frases se arman aca y no en el panel: si el relato se escribe en el
     * cliente, dos clientes cuentan el mismo episodio distinto, y este texto
     * termina citado en una discusion clinica. */
    private fun describe(s: SentinelSignal): String = when (s.type?.name) {
        "EPISODE_OPENED" ->
            "Se abrió el episodio" + (s.trigger?.let { " por ${humanState(it)}" } ?: "")
        "EPISODE_ESCALATED", "SEVERITY_RAMP" ->
            "Subió de severidad" + (s.severity?.let { " a ${it.lowercase()}" } ?: "")
        "UMBRELLA_EVENT" ->
            if (s.triggerOn == "COME_BACK")
                "Venció el plazo de retorno" + (s.state?.let { " a ${humanState(it)}" } ?: "")
            else
                "Movimiento dentro del episodio" + (s.state?.let { " (${humanState(it)})" } ?: "")
        "EPISODE_CLOSED" -> when (s.cause) {
            "AUTO_RECOVERY" -> "Volvió sola a una posición segura y el episodio cerró"
            "STAFF", "STAFF_ASSIST" -> "Cerró cuando llegó el personal"
            else -> "El episodio cerró" + (s.cause?.let { " ($it)" } ?: "")
        }
        else -> s.type?.name ?: "UNKNOWN"
    }

    private fun describe(c: SceneEvent): String = when (c.eventType?.name) {
        "TransitionDetected" -> "${humanState(c.fromState?.name)} → ${humanState(c.toState?.name)}"
        "ComeBackExceeded" -> "No volvió al estado de referencia dentro del plazo"
        "DwellExceeded" -> "Se quedó más tiempo del tolerado"
        else -> c.eventType?.name ?: "UNKNOWN"
    }

    /* Los nombres del motor son del motor. Aca se traducen una vez, en el borde,
     * porque el que lee esto es un director medico y no un ingeniero. */
    private fun humanState(raw: String?): String = when (raw?.uppercase()) {
        null -> "sin determinar"
        "LYING" -> "acostado"
        "SITTINGINBED", "SITTING_IN_BED" -> "incorporado en la cama"
        "ATTEMPTINGEXIT", "ATTEMPTING_EXIT" -> "intentando salir"
        "BEDEDGE", "BED_EDGE" -> "al borde de la cama"
        "STANDING" -> "de pie"
        "ONFLOOR", "ON_FLOOR" -> "en el piso"
        "INBATHROOM", "IN_BATHROOM" -> "en el baño"
        "INROOM", "IN_ROOM" -> "en la habitación"
        "INHALLWAY", "IN_HALLWAY" -> "en el pasillo"
        "OUTDOOR" -> "afuera"
        "ABSENT" -> "fuera de la habitación"
        "INCHAIR", "IN_CHAIR" -> "en una silla"
        "INWHEELCHAIR", "IN_WHEELCHAIR" -> "en la silla de ruedas"
        "UNKNOWN" -> "sin determinar"
        else -> raw.lowercase()
    }
}
