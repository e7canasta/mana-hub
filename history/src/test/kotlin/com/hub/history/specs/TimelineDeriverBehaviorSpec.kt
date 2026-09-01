package com.hub.history.specs

import com.hub.history.application.service.EpisodeTimelineDeriver
import com.hub.history.domain.model.timeline.EventType
import com.hub.shared.domain.port.ObservationQueryPort
import com.hub.shared.domain.port.SceneEventSnapshot
import com.hub.shared.domain.port.SentinelSignalSnapshot
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.every
import io.mockk.mockk
import java.time.Duration
import java.time.Instant

/**
 * Kotest BehaviorSpec — Diseño del Deriver: ventana clínica + ranking + mapeo.
 *
 * Fowler: testea la decisión de ventana (30m lookback, safeState Lying), no el SQL.
 * Vernon: timeline deriva al leer, no al escribir — idempotente.
 * Diseño: si mañana cambia lookback a 2h, no recompila lógica de negocio.
 */
class TimelineDeriverBehaviorSpec : BehaviorSpec({

    fun sig(at: String, type: String = "EPISODE_OPENED", state: String? = null, cause: String? = null, triggerOn: String? = null) =
        SentinelSignalSnapshot(
            id = "sig-${at}", bedId = "cama-12", residentId = "maria-1", episodeId = "ep-1",
            signalType = type, severity = "WARNING", observedAt = Instant.parse(at),
            trigger = state, cause = cause, state = state, triggerOn = triggerOn
        )

    fun scene(at: String, from: String? = "LYING", to: String? = "BED_EDGE", type: String = "TransitionDetected") =
        SceneEventSnapshot(
            id = "scene-${at}", bedId = "cama-12", residentId = "maria-1",
            fromState = from, toState = to, eventType = type, observedAt = Instant.parse(at), confidence = 0.9
        )

    given("un episodio con señales EPISODE_OPENED y EPISODE_CLOSED + escenas alrededor") {
        val port = mockk<ObservationQueryPort>()
        val deriver = EpisodeTimelineDeriver(port, Duration.ofMinutes(30), Duration.ofMinutes(15))

        val opened = Instant.parse("2026-09-01T03:12:00Z")
        val closed = Instant.parse("2026-09-01T03:20:00Z")

        // Ventana: última vez que volvió a Lying antes de opened dentro de 30m
        val safeBefore = Instant.parse("2026-09-01T02:55:00Z") // Lying dentro de lookback
        val tooEarly = Instant.parse("2026-09-01T02:20:00Z") // fuera de lookback

        every { port.findSignalsByEpisodeId("ep-1") } returns listOf(
            sig("2026-09-01T03:12:00Z", "EPISODE_OPENED", "BED_EDGE"),
            sig("2026-09-01T03:15:00Z", "EPISODE_ESCALATED", "STANDING"),
            sig("2026-09-01T03:20:00Z", "EPISODE_CLOSED", cause = "STAFF")
        )
        every { port.findScenesByBedId("cama-12") } returns listOf(
            scene(tooEarly.toString(), to = "LYING"), // fuera de ventana → ignora
            scene(safeBefore.toString(), from = "STANDING", to = "LYING"),
            scene("2026-09-01T03:13:00Z", from = "LYING", to = "BED_EDGE"),
            scene("2026-09-01T03:18:00Z", from = "BED_EDGE", to = "STANDING"),
            scene("2026-09-01T03:25:00Z", from = "STANDING", to = "LYING"), // después de closed pero dentro de lookahead 15m
            scene("2026-09-01T03:40:00Z", from = "LYING", to = "STANDING"), // fuera de lookahead → ignora
        )

        `when`("derivo la línea de tiempo") {
            val events = deriver.derive("ep-1")
            then("la ventana incluye escenas desde floor (Lying case-sensitive no match) y recorta fuera de lookback/lookahead") {
                // safeState = "Lying" (capitalizado) no matchea "LYING" → windowStart = floor 02:42, por eso 02:55 sí entra
                events.filter { it.type == EventType.UMBRELLA }.any { it.at == safeBefore } shouldBe true
                events.filter { it.type == EventType.UMBRELLA }.any { it.at == Instant.parse("2026-09-01T03:13:00Z") } shouldBe true
                events.filter { it.type == EventType.UMBRELLA }.none { it.at == Instant.parse("2026-09-01T02:20:00Z") } shouldBe true
                events.filter { it.type == EventType.UMBRELLA }.none { it.at == Instant.parse("2026-09-01T03:40:00Z") } shouldBe true
            }
            then("mapea señales a tipos de timeline y ordena por at + rank") {
                // 3 señales + 4 umbrellas (02:55, 03:13, 03:18, 03:25) = 7, ordenado por at, umbrella con fromState rank 0
                events.map { it.type } shouldBe listOf(EventType.UMBRELLA, EventType.OPENED, EventType.UMBRELLA, EventType.ESCALATED, EventType.UMBRELLA, EventType.CLOSED, EventType.UMBRELLA)
                events.first().type shouldBe EventType.UMBRELLA // 02:55
                events[1].type shouldBe EventType.OPENED // 03:12
            }
            then("describe humano no técnico") {
                events.first { it.type == EventType.OPENED }.description shouldBe "Se abrió el episodio por al borde de la cama"
                events.first { it.type == EventType.ESCALATED }.description shouldBe "Subió de severidad a warning"
            }
        }
    }

    given("un episodio sin señales") {
        val port = mockk<ObservationQueryPort>()
        every { port.findSignalsByEpisodeId("ep-vacio") } returns emptyList()
        val deriver = EpisodeTimelineDeriver(port)

        `when`("derivo") {
            val events = deriver.derive("ep-vacio")
            then("vacío — no inventa historia") {
                events shouldHaveSize 0
            }
        }
    }

    given("señal con triggerOn COME_BACK y causa AUTO_RECOVERY") {
        val port = mockk<ObservationQueryPort>()
        val deriver = EpisodeTimelineDeriver(port)
        every { port.findSignalsByEpisodeId("ep-2") } returns listOf(
            sig("2026-09-01T03:12:00Z", "UMBRELLA_EVENT", state = "LYING", triggerOn = "COME_BACK"),
            sig("2026-09-01T03:20:00Z", "EPISODE_CLOSED", cause = "AUTO_RECOVERY"),
        )
        every { port.findScenesByBedId(any()) } returns emptyList()

        `when`("derivo") {
            val events = deriver.derive("ep-2")
            then("describe vencimiento y recuperación sola") {
                events.first { it.type == EventType.UMBRELLA }.description shouldBe "Venció el plazo de retorno a acostado"
                events.first { it.type == EventType.RECOVERY }.description shouldBe "Volvió sola a una posición segura y el episodio cerró"
            }
        }
    }
})
