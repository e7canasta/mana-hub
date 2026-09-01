package com.hub.observation.specs

import com.hub.observation.domain.model.*
import com.hub.observation.support.*
import com.hub.shared.domain.ResidentId
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDate

/**
 * Kotest BehaviorSpec — Resúmenes clínicos diarios.
 *
 * Director: "¿Cómo durmió María? ¿Cuánto caminó? ¿Cuántas visitas al baño?"
 * Testea upsert clínico (copy inmutable), no getters.
 */
class ResumenClinicoBehaviorSpec : BehaviorSpec({

    given("María durmió 2026-09-01 fragmentado con 2 salidas") {
        `when`("el motor ingesta resumen de sueño") {
            val r = resumenSueno(calma = 240, inquieto = 60, despierto = 30, salidas = 2)
            then("registra calma/inquieto/despierto y salidas con ventana horaria V8") {
                r.calmMinutes shouldBe 240
                r.restlessMinutes shouldBe 60
                r.bedExitCount shouldBe 2
                r.startedAt?.hour shouldBe 22
                r.endedAt?.hour shouldBe 6
            }
        }
        `when`("llega segundo resumen para el mismo día corregido") {
            val dia = LocalDate.of(2026, 9, 1)
            val original = resumenSueno(residente = "maria", dia = dia, salidas = 1, calma = 200)
            val corregido = original.copy(bedExitCount = 3, calmMinutes = 250, sourceRecordId = "sleep-corr")
            then("inmutable: original intacto, corregido con nuevos valores") {
                original.bedExitCount shouldBe 1
                corregido.bedExitCount shouldBe 3
                corregido.observedOn shouldBe dia
            }
        }
    }

    given("un día con movilidad") {
        `when`("hay caminata y 3 transferencias") {
            val r = resumenMovilidad()
            then("registra distancia y transferencias") {
                r.walkingMinutes shouldBe 30
                r.distanceMeters shouldBe 120.0
                r.transferCount shouldBe 3
            }
        }
        `when`("día inmóvil") {
            val r = MobilitySummary.create(
                sourceRecordId = "mob-0", residentId = ResidentId("r1"),
                observedOn = LocalDate.of(2026, 9, 2),
                inBedMinutes = 720, outOfBedMinutes = 0, outOfSightMinutes = 0,
                walkingMinutes = 0, distanceMeters = 0.0, transferCount = 0,
                source = "model", modelVersion = "2.1", confidence = 1.0
            )
            then("cero es válido") {
                r.walkingMinutes shouldBe 0
            }
        }
    }

    given("visitas al baño") {
        `when`("4 visitas con 2 nocturnas y 1 asistida") {
            val r = resumenBano()
            then("frecuencia y asistencia quedan") {
                r.visitCount shouldBe 4
                r.nightVisitCount shouldBe 2
                r.assistedCount shouldBe 1
            }
        }
        `when`("noche sin visitas") {
            val r = BathroomSummary.create(
                sourceRecordId = "bath-0", residentId = ResidentId("r2"),
                observedOn = LocalDate.of(2026, 9, 3),
                visitCount = 0, nightVisitCount = 0, assistedCount = 0, totalMinutes = 0,
                source = "model", modelVersion = "2.1", confidence = 0.95
            )
            then("cero visitas es válido") {
                r.visitCount shouldBe 0
            }
        }
    }
})
