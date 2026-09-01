package com.hub.evidence.specs

import com.hub.evidence.domain.model.ClipWindow
import com.hub.evidence.domain.model.Evidence
import com.hub.evidence.domain.model.Timeline
import com.hub.shared.domain.BedId
import com.hub.shared.domain.ResidentId
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.assertions.throwables.shouldThrow
import java.time.Instant

/**
 * Kotest BehaviorSpec — Evidencia: timeline y clipWindow como memoria legal.
 *
 * Director/legal: "¿Qué ventana de video guardamos? ¿El timeline está abierto o cerrado? ¿Se puede cerrar dos veces?"
 *
 * Vernon: Timeline y ClipWindow son Aggregates con ciclo open→closed y invariante isOpen.
 */
class EvidenciaBehaviorSpec : BehaviorSpec({

    given("evidencia puntual de un evento") {
        `when`("se crea evidencia de tipo CAMERA para María") {
            val ev = Evidence.create(
                bedId = BedId("cama-12"), residentId = ResidentId("maria-1"),
                evidenceType = "CAMERA", category = "BED_EDGE", timestamp = Instant.parse("2026-09-01T03:12:00Z")
            )
            then("queda con tipo y categoría sin clip asociado aún") {
                ev.evidenceType shouldBe "CAMERA"
                ev.category shouldBe "BED_EDGE"
                ev.sceneEventId shouldBe null
            }
        }
    }

    given("un timeline abierto para reconstruir lo que pasó") {
        val tl = Timeline.create(
            bedId = BedId("cama-12"), residentId = ResidentId("maria-1"),
            windowStart = Instant.parse("2026-09-01T03:10:00Z")
        )
        `when`("se consulta isOpen") {
            then("está abierto — aún recolecta eventos antes/después") {
                tl.isOpen shouldBe true
                tl.closedAt shouldBe null
            }
        }
        `when`("se cierra") {
            val cerrado = tl.close()
            then("queda cerrado con timestamp y versión+1") {
                cerrado.isOpen shouldBe false
                cerrado.closedAt shouldNotBe null
                // no muta original
                tl.isOpen shouldBe true
            }
        }
        `when`("se intenta cerrar dos veces") {
            val cerrado = tl.close()
            then("falla — no se cierra lo cerrado") {
                shouldThrow<IllegalArgumentException> { cerrado.close() }
            }
        }
    }

    given("una ventana de clip para el NVR") {
        val clip = ClipWindow.create(
            bedId = BedId("cama-12"), residentId = ResidentId("maria-1"), timeoutMinutes = 5
        )
        `when`("se crea") {
            then("queda open con timeout 5 y sin endedAt") {
                clip.isOpen shouldBe true
                clip.state shouldBe "open"
                clip.endedAt shouldBe null
                clip.timeoutMinutes shouldBe 5
            }
        }
        `when`("se cierra por condición") {
            val cerrado = clip.close()
            then("pasa a closed con endedAt y closedAt") {
                cerrado.isOpen shouldBe false
                cerrado.state shouldBe "closed"
                cerrado.endedAt shouldNotBe null
                cerrado.closedAt shouldNotBe null
            }
        }
        `when`("se intenta cerrar un clip ya cerrado") {
            val cerrado = clip.close()
            then("falla invariante") {
                shouldThrow<IllegalArgumentException> { cerrado.close() }
            }
        }
    }

    given("reconstitución desde BD no emite lógica") {
        `when`("hidrato timeline cerrado") {
            val id = com.hub.evidence.domain.model.EvidenceId.random()
            val rec = Timeline.reconstitute(
                id, BedId("cama-12"), ResidentId("maria-1"), null, null, "[]", "[]",
                Instant.parse("2026-09-01T03:10:00Z"), Instant.parse("2026-09-01T03:15:00Z"), Instant.now(), 3
            )
            then("respeta versión y estado cerrado") {
                rec.id shouldBe id
                rec.version shouldBe 3L
                rec.isOpen shouldBe false
            }
        }
    }
})
