package com.hub.surveillance.specs

import com.hub.shared.domain.BedId
import com.hub.shared.domain.EpisodeId
import com.hub.shared.domain.ResidentId
import com.hub.surveillance.domain.model.Episode
import com.hub.surveillance.domain.model.EpisodeSeverity
import com.hub.surveillance.domain.model.EpisodeStatus
import com.hub.surveillance.support.*
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.assertions.throwables.shouldThrow
import java.time.Instant

/**
 * Kotest BehaviorSpec — Vigilancia: ciclo de episodio + escalamiento.
 *
 * Guardia: "Se abrió WARNING, se reconoció, escaló a supervisor, se complicó a CRITICAL".
 */
class VigilanciaBehaviorSpec : BehaviorSpec({

    given("el motor externo abre un episodio CRITICAL para María") {
        `when`("se crea desde SEÑAL") {
            val ep = Episode.fromSignal(
                residentId = ResidentId("maria-1"), bedId = BedId("cama-12"),
                signalType = "DWELL", severity = EpisodeSeverity.CRITICAL,
                title = "Caída inminente", detail = "5 min borde", occurredAt = Instant.parse("2026-09-01T03:12:00Z")
            )
            then("nace PENDING con evento Created") {
                ep.status shouldBe EpisodeStatus.PENDING
                ep.severity shouldBe EpisodeSeverity.CRITICAL
                ep.escalationLevel shouldBe 0
                ep.domainEvents.any { it.eventType == "EpisodeCreated" } shouldBe true
            }
        }
    }

    given("un episodio pendiente") {
        val ep = episodio { residente = "maria-1"; gravedad = EpisodeSeverity.WARNING }
        `when`("la enfermera Ana lo reconoce") {
            val rec = ep.acknowledge("enfermera.ana")
            then("pasa a ACKNOWLEDGED con actor y no muta el original") {
                rec.status shouldBe EpisodeStatus.ACKNOWLEDGED
                rec.statusActorId shouldBe "enfermera.ana"
                ep.status shouldBe EpisodeStatus.PENDING
            }
        }
        `when`("intenta reconocer dos veces") {
            val rec = ep.acknowledge("ana")
            then("falla — protege la guardia") {
                shouldThrow<IllegalArgumentException> { rec.acknowledge("otra") }
            }
        }
    }

    given("un episodio que debe escalar") {
        `when`("no se resuelve en 10m y escala a supervisor") {
            val esc = episodio { gravedad = EpisodeSeverity.WARNING }.escalate("supervisor.noche")
            then("nivel 1 con evento") {
                esc.escalationLevel shouldBe 1
                esc.escalatedTo shouldBe "supervisor.noche"
                esc.domainEvents.any { it.eventType == "EpisodeEscalated" } shouldBe true
            }
        }
        `when`("doble escalamiento") {
            val e2 = episodio { }.escalate("supervisor").escalate("director")
            then("acumula niveles") {
                e2.escalationLevel shouldBe 2
            }
        }
    }

    given("un cuadro que empeora") {
        `when`("WARNING → CRITICAL con detalle") {
            val elevado = episodio { gravedad = EpisodeSeverity.WARNING; detalle = "borde" }
                .elevateSeverity(EpisodeSeverity.CRITICAL, "ahora cayó")
            then("eleva y guarda nuevo detalle") {
                elevado.severity shouldBe EpisodeSeverity.CRITICAL
                elevado.detail shouldBe "ahora cayó"
            }
        }
        `when`("intenta degradar CRITICAL → WARNING") {
            val mismo = episodio { gravedad = EpisodeSeverity.CRITICAL }
                .elevateSeverity(EpisodeSeverity.WARNING, "bajar")
            then("no retrocede — Vernon protege invariante") {
                mismo.severity shouldBe EpisodeSeverity.CRITICAL
            }
        }
        `when`("se complica a EMERGENCY y escala") {
            val comp = episodio { gravedad = EpisodeSeverity.WARNING }
                .complicated(EpisodeSeverity.EMERGENCY, "guardia", "caída con golpe")
            then("emergencia nivel 1") {
                comp.severity shouldBe EpisodeSeverity.EMERGENCY
                comp.escalationLevel shouldBe 1
            }
        }
    }

    given("reconstitución desde BD") {
        `when`("hidrato episodio ACKNOWLEDGED nivel 2") {
            val id = EpisodeId.random()
            val rec = Episode.reconstitute(
                id, ResidentId("maria-1"), BedId("cama-12"), "video", "clip-123", "BED_EDGE",
                EpisodeSeverity.WARNING, EpisodeStatus.ACKNOWLEDGED, "ana", Instant.now(),
                "Borde", "detalle", Instant.now(), 2, Instant.now(), "supervisor", 5
            )
            then("respeta estado sin emitir eventos") {
                rec.id shouldBe id
                rec.escalationLevel shouldBe 2
                rec.domainEvents.size shouldBe 0
            }
        }
    }
})
