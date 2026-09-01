package com.hub.care.specs

import com.hub.care.domain.model.*
import com.hub.care.support.*
import com.hub.shared.domain.ResidentId
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import java.time.LocalDate

/**
 * Kotest BehaviorSpec — Ronda + hallazgo clínico.
 *
 * Enfermería: "Ronda 22:00 ala Norte, visito cama-12, dejo nota CLINICAL, cierro tarea".
 */
class CuidadoBehaviorSpec : BehaviorSpec({

    given("ronda programada ala Norte 22:00") {
        `when`("se crea") {
            val r = ronda { ala = "ala-norte" }
            then("nace IN_PROGRESS con evento Started") {
                r.status shouldBe RoundStatus.IN_PROGRESS
                r.isInProgress shouldBe true
                r.domainEvents.any { it.eventType == "RoundStarted" } shouldBe true
            }
        }
    }

    given("una ronda en progreso con una tarea para María en cama-12") {
        val r = ronda { }
        val t = tareaDeRonda(r.id, "maria-1", "cama-12")
        `when`("la enfermera visita y deja nota") {
            val hecha = t.complete("Paciente tranquila", "enfermera.ana")
            then("tarea COMPLETED con nota y actor") {
                hecha.status shouldBe RoundTaskStatus.COMPLETED
                hecha.note shouldBe "Paciente tranquila"
                hecha.completedBy shouldBe "enfermera.ana"
            }
        }
        `when`("intenta completar dos veces") {
            val hecha = t.complete("ok", "ana")
            then("falla") {
                shouldThrow<IllegalArgumentException> { hecha.complete("otra", "ana") }
            }
        }
    }

    given("cierre de ronda") {
        `when`("se completa") {
            val cerrada = ronda { }.complete("enfermera.ana")
            then("COMPLETED con evento") {
                cerrada.status shouldBe RoundStatus.COMPLETED
                cerrada.domainEvents.any { it.eventType == "RoundCompleted" } shouldBe true
            }
        }
        `when`("se cancela") {
            val anulada = ronda { }.cancel()
            then("CANCELLED") {
                anulada.status shouldBe RoundStatus.CANCELLED
            }
        }
        `when`("ya completada intenta completar de nuevo") {
            val cerrada = ronda { }.complete("ana")
            then("falla invariante") {
                shouldThrow<IllegalArgumentException> { cerrada.complete("ana") }
            }
        }
    }

    given("hallazgo clínico del modelo") {
        `when`("ML detecta patrón de salidas") {
            val h = notaDe("Salidas 5x/semana al alba 05:00-06:05")
            then("queda INSIGHT con autor ml-model") {
                h.kind shouldBe CareNoteKind.INSIGHT
                h.authorId shouldBe "ml-model"
            }
        }
        `when`("enfermera escribe nota CLINICAL") {
            val n = notaClinica(residente = "maria-1", autor = "dra.garcia", tipo = CareNoteKind.CLINICAL, texto = "Patrón 5x")
            then("guarda tipo y duración") {
                n.kind shouldBe CareNoteKind.CLINICAL
                n.durationMin shouldBe 5
            }
        }
    }

    given("resumen diario de cuidado") {
        `when`("120 min con 90 proactivos") {
            val r = CareSummary.create(
                sourceRecordId = "cs-1", residentId = ResidentId("maria-1"),
                observedOn = LocalDate.of(2026, 9, 1), totalMinutes = 120, proactiveMinutes = 90, roundsCount = 3, notesCount = 5
            )
            then("válido y con replay") {
                r.totalMinutes shouldBe 120
                val upd = r.replay("cs-1", 140, 100, 3, 5, "model", "1.1", 0.9)
                upd.totalMinutes shouldBe 140
            }
        }
        `when`("proactivos > total") {
            then("falla invariante") {
                shouldThrow<IllegalArgumentException> {
                    CareSummary.create("x", ResidentId("r1"), LocalDate.now(), 60, 80, 1, 1)
                }
            }
        }
    }

    given("nota de episodio al reconocer") {
        `when`("enfermera hace ACK") {
            val n = EpisodeNote.create(com.hub.shared.domain.EpisodeId.random(), "ana", EpisodeNoteKind.ACKNOWLEDGEMENT, "Recibido 03:13")
            then("kind ACKNOWLEDGEMENT") {
                n.kind shouldBe EpisodeNoteKind.ACKNOWLEDGEMENT
            }
        }
    }
})
