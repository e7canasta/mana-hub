package com.hub.history.specs

import com.hub.history.domain.model.*
import com.hub.history.domain.model.timeline.*
import com.hub.shared.domain.BedId
import com.hub.shared.domain.ResidentId
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.assertions.throwables.shouldThrow
import java.time.Instant

/**
 * Kotest BehaviorSpec — Historia clínica: episodio normalizado + revisión + línea de tiempo.
 *
 * Director/auditor: "¿Qué pasó con María el 2026-09-01? ¿Quién revisó y qué veredicto? ¿La línea de tiempo está ordenada?"
 *
 * Vernon: HistoryEpisode Aggregate con invariantes (sourceRecordId, far future).
 * Fowler: No testear getters — testear reloj inyectado (HistoryEpisodeReview.kt:19) y sortedByTime.
 */
class HistoriaClinicaBehaviorSpec : BehaviorSpec({

    given("un episodio histórico de caída para María") {
        `when`("el EpisodeEngine lo normaliza") {
            val ep = HistoryEpisode.create(
                sourceRecordId = "evt-123", residentId = ResidentId("maria-1"),
                bedId = BedId("cama-12"), kind = EpisodeKind.FALL,
                severity = HistoryEpisodeSeverity.CRITICAL,
                occurredAt = Instant.parse("2026-09-01T03:12:00Z"),
                source = EventSource.SENTINEL
            )
            then("queda con kind FALL y severidad CRITICAL, sin narrativa aún") {
                ep.kind shouldBe EpisodeKind.FALL
                ep.severity shouldBe HistoryEpisodeSeverity.CRITICAL
                ep.source shouldBe EventSource.SENTINEL
                ep.narrative shouldBe null
            }
        }
        `when`("sourceRecordId está vacío") {
            then("falla — no se puede auditar sin origen") {
                shouldThrow<IllegalArgumentException> {
                    HistoryEpisode.create("", ResidentId("maria-1"), null, EpisodeKind.OTHER, HistoryEpisodeSeverity.INFO, Instant.now(), EventSource.MANUAL)
                }
            }
        }
        `when`("occurredAt está en futuro lejano") {
            then("falla — no se puede registrar mañana") {
                shouldThrow<IllegalArgumentException> {
                    HistoryEpisode.create("id", ResidentId("maria-1"), null, EpisodeKind.FALL, HistoryEpisodeSeverity.CRITICAL, Instant.now().plusSeconds(7200), EventSource.SENTINEL)
                }
            }
        }
    }

    given("una revisión que debe inyectar el reloj") {
        val epId = HistoryEpisodeId.random()
        `when`("la auditora revisa como 'resolved' con reloj inyectado 2026-09-02T10:00Z") {
            val ahora = Instant.parse("2026-09-02T10:00:00Z")
            val rev = HistoryEpisodeReview.create(epId, actorId = "auditora.ana", now = ahora, status = "resolved", detectionVerdict = "true_positive")
            then("resolvedAt es el reloj inyectado, no Instant.now(), y veredicto queda") {
                rev.resolvedAt shouldBe ahora
                rev.detectionVerdict shouldBe "true_positive"
                rev.status shouldBe "resolved"
            }
        }
        `when`("queda pendiente") {
            val rev = HistoryEpisodeReview.create(epId, actorId = "ana", now = Instant.now(), status = "pending")
            then("resolvedAt es null — aún no resuelta") {
                rev.resolvedAt shouldBe null
            }
        }
        `when`("reconstituyo desde BD") {
            val id = HistoryEpisodeId.random()
            val rec = HistoryEpisodeReview.reconstitute(id, epId, "resolved", "true_positive", "ok", Instant.now(), "ana", 2)
            then("respeta versión sin emitir evento") {
                rec.id shouldBe id
                rec.version shouldBe 2L
            }
        }
    }

    given("intervención de enfermería") {
        `when`("se registra asistencia a las 03:20") {
            val interv = HistoryEpisodeIntervention.create(
                episodeId = HistoryEpisodeId.random(), kind = InterventionKind.STAFF_DISPATCHED,
                performedAt = Instant.parse("2026-09-01T03:20:00Z"), detail = "Ayuda a reincorporarse"
            )
            then("queda con kind y detalle") {
                interv.kind shouldBe InterventionKind.STAFF_DISPATCHED
                interv.detail shouldBe "Ayuda a reincorporarse"
            }
        }
        `when`("performedAt está en futuro lejano") {
            then("falla — no se puede intervenir mañana") {
                shouldThrow<IllegalArgumentException> {
                    HistoryEpisodeIntervention.create(HistoryEpisodeId.random(), InterventionKind.STAFF_DISPATCHED, Instant.now().plusSeconds(7200))
                }
            }
        }
    }

    given("línea de tiempo de un episodio con eventos desordenados") {
        val epId = "ep-123"
        val resident = ResidentId("maria-1")
        val events = listOf(
            EpisodeTimelineEvent(EpisodeTimelineEventId.random(), epId, resident, Instant.parse("2026-09-01T03:15:00Z"), EventType.ESCALATED, "BED_EDGE", "STANDING", null),
            EpisodeTimelineEvent(EpisodeTimelineEventId.random(), epId, resident, Instant.parse("2026-09-01T03:12:00Z"), EventType.OPENED, "LYING", "BED_EDGE", null),
            EpisodeTimelineEvent(EpisodeTimelineEventId.random(), epId, resident, Instant.parse("2026-09-01T03:20:00Z"), EventType.RESPONDED, null, null, "asistencia"),
        )
        `when`("construyo timeline sin ordenar") {
            val tl = EpisodeTimeline(epId, resident, events)
            then("sortedByTime lo deja cronológico para el auditor") {
                val sorted = tl.sortedByTime()
                sorted.events.first().at shouldBe Instant.parse("2026-09-01T03:12:00Z")
                sorted.events.last().at shouldBe Instant.parse("2026-09-01T03:20:00Z")
                // original no muta — inmutable
                tl.events.first().at shouldBe Instant.parse("2026-09-01T03:15:00Z")
            }
        }
    }

    given("vocabulario de kinds que el director ve") {
        `when`("EpisodeKind from") {
            then("FALL/BED_EXIT toleran mayúsculas, inventado → OTHER") {
                EpisodeKind.from("fall") shouldBe EpisodeKind.FALL
                EpisodeKind.from("BED_EXIT") shouldBe EpisodeKind.BED_EXIT
                EpisodeKind.from("inventado") shouldBe EpisodeKind.OTHER
            }
        }
    }
})
