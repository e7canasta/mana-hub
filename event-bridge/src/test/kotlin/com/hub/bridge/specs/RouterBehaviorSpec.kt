package com.hub.bridge.specs

import com.hub.bridge.ingest.EventRouter
import com.manahive.contracts.EventEnvelope
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.web.client.RestClient

/**
 * Kotest BehaviorSpec — EventRouter: qué subject hace forward y qué no.
 *
 * SOR: el bridge no deserializa, solo enruta raw JSON por subject.
 * Fowler: test de decisión (branch), no de dato. Vernon: Anti-corruption layer.
 */
class RouterBehaviorSpec : BehaviorSpec({

    fun envelope(type: String = "DWELL", payload: String = """{"x":1}""") =
        EventEnvelope(eventId = "evt-1", type = type, version = 1, occurredAt = java.time.Instant.now(), source = "test", payloadJson = payload)

    given("un EventRouter con RestClient mockeado") {
        val mockRest = mockk<RestClient>(relaxed = true)
        val mockBuilder = mockk<RestClient.RequestBodyUriSpec>(relaxed = true)
        val mockRetrieve = mockk<RestClient.ResponseSpec>(relaxed = true)
        every { mockRest.post() } returns mockBuilder
        every { mockBuilder.uri(any<String>()) } returns mockBuilder
        every { mockBuilder.header(any(), any()) } returns mockBuilder
        every { mockBuilder.body(any<String>()) } returns mockBuilder
        every { mockBuilder.retrieve() } returns mockRetrieve
        every { mockRetrieve.toEntity(String::class.java) } returns org.springframework.http.ResponseEntity.ok("ok")

        val router = EventRouter(mockRest)

        `when`("llega scene.update en subject scene.cama-12") {
            router.route(envelope("STATE_CHANGED"), "scene.cama-12")
            then("hace forward a /internal/v1/integration/scene-events") {
                verify { mockBuilder.uri("/internal/v1/integration/scene-events") }
            }
        }

        `when`("llega sentinel en subject sentinel.maria") {
            router.route(envelope("EPISODE_OPENED"), "sentinel.maria")
            then("hace forward a /internal/v1/integration/signal-events") {
                verify { mockBuilder.uri("/internal/v1/integration/signal-events") }
            }
        }

        `when`("llega perception, alarm, hub, recorder, evidence") {
            // estos se skipean — SOR no los persiste vía bridge
            router.route(envelope(), "perception.cam-1")
            router.route(envelope(), "alarm.bed-edge")
            router.route(envelope(), "hub.profile-changed")
            router.route(envelope(), "recorder.clip")
            router.route(envelope(), "evidence.timeline")
            then("no hace forward — solo log debug") {
                verify(exactly = 1) { mockBuilder.uri("/internal/v1/integration/scene-events") }
                verify(exactly = 1) { mockBuilder.uri("/internal/v1/integration/signal-events") }
            }
        }

        `when`("llega subject desconocido") {
            router.route(envelope(), "unknown.foo")
            then("skipea sin forward") {
                verify(exactly = 1) { mockBuilder.uri("/internal/v1/integration/scene-events") }
            }
        }
    }
})
