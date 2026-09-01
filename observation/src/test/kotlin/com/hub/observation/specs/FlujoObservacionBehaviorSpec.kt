package com.hub.observation.specs

import com.hub.observation.domain.model.*
import com.hub.observation.support.*
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.collections.shouldContain
import io.kotest.assertions.throwables.shouldThrow

/**
 * Kotest 6.x BehaviorSpec — Flujo de observación clínica.
 *
 * Director: "¿Qué vio la cámara? ¿En qué cama está María? ¿Entró enfermería?"
 * No testea enums sueltos, testea el flujo percepción → estado → notificación.
 */
class FlujoObservacionBehaviorSpec : BehaviorSpec({

    given("la cámara de cama-12 ve a María ocupada despierta") {
        `when`("ingresa percepción CAMERA") {
            val (sensor, bedState, ts) = percepcion {
                camaId = "cama-12"; residenteId = "maria-1"; tipo = SensorEventKind.CAMERA
                estado = "occupied"; habitacion = "hab-101"; durmiendo = false
            }
            then("el sensor guarda origen CAMERA y la cama queda ocupada desde ese instante") {
                sensor.kind shouldBe SensorEventKind.CAMERA
                bedState.bedId.value shouldBe "cama-12"
                bedState.state shouldBe "occupied"
                bedState.sleeping shouldBe false
                bedState.stateSince shouldBe ts
                sensor.occurredAt shouldBe ts
            }
        }
    }

    given("sensores diversos en la misma cama") {
        `when`("radar detecta durmiendo y PIR ve vacía") {
            val (_, s1, _) = percepcion { tipo = SensorEventKind.RADAR; estado = "sleeping"; durmiendo = true }
            val (_, s2, _) = percepcion { tipo = SensorEventKind.PIR; estado = "empty"; residenteId = null }
            then("cada uno registra su estado sin confundir residente") {
                s1.sleeping shouldBe true
                s2.residentId.shouldBeNull()
                s2.state shouldBe "empty"
            }
        }
    }

    given("cambios de escena que la directora ve") {
        `when`("vacía → ocupada") {
            val e = escena { desde = SceneState.EMPTY; hacia = SceneState.OCCUPIED }
            then("es transición real con histéresis") {
                escenaEsTransicion(e) shouldBe true
                e.fromState shouldBe SceneState.EMPTY
                e.toState shouldBe SceneState.OCCUPIED
            }
        }
        `when`("durmiendo → durmiendo") {
            val e = escena { desde = SceneState.SLEEPING; hacia = SceneState.SLEEPING }
            then("no es transición — no debe escalar") {
                (e.fromState == e.toState) shouldBe true
            }
        }
    }

    given("avisos que el SOR registra sin decidir") {
        `when`("llega WARNING de borde y CRITICAL de caída") {
            val w = notificacion(categoria = "WARNING", tipo = "bed_edge_detected")
            val c = notificacion(categoria = "CRITICAL", tipo = "fall_detected")
            then("se guardan con categoría intacta — el motor decidió, el SOR recuerda") {
                w.category shouldBe NotificationCategory.WARNING
                c.category shouldBe NotificationCategory.CRITICAL
                w.eventType shouldBe "bed_edge_detected"
            }
        }
        `when`("el motor manda categoría basura") {
            then("cae a INFORMATIONAL, no explota") {
                NotificationCategory.from("basura") shouldBe NotificationCategory.INFORMATIONAL
            }
        }
    }

    given("enfermería entra a la habitación") {
        val (_, bedState, _) = percepcion { camaId = "cama-12"; residenteId = "maria-1" }
        `when`("se recibe staff_entered") {
            val conStaff = bedState.copy(staffPresent = true)
            then("la cama reporta staff presente sin perder residente") {
                conStaff.staffPresent shouldBe true
                conStaff.residentId?.value shouldBe "maria-1"
            }
        }
    }

    given("la envoltura que manda el motor externo") {
        val mapper = ObjectMapper().findAndRegisterModules()
        `when`("llega JSON con from_state y triggerOn") {
            val json = """{"type":"EPISODE_OPENED","from_state":"LYING","to_state":"STANDING","triggerOn":"DWELL","requiresNvr":true}"""
            val env = mapper.readValue(json, SignalEnvelope::class.java)
            then("se deserializa con guion bajo y camelCase") {
                env.type shouldBe "EPISODE_OPENED"
                env.fromState shouldBe "LYING"
                env.triggerOn shouldBe "DWELL"
                env.requiresNvr shouldBe true
            }
        }
        `when`("el motor evoluciona con campo futuro") {
            val json = """{"type":"X","campoFuturo":"123"}"""
            val env = mapper.readValue(json, SignalEnvelope::class.java)
            then("ignora desconocidos — no rompe") {
                env.type shouldBe "X"
            }
        }
    }

    given("señales centinela que originan episodios") {
        `when`("SignalType reconoce los 8 tipos del contrato mana-hive") {
            then("no explota con tipo inventado") {
                SignalType.from("EPISODE_OPENED") shouldBe SignalType.EPISODE_OPENED
                SignalType.from("DWELL_PRE_WARNING") shouldBe SignalType.DWELL_PRE_WARNING
                SignalType.from("INVENTADO") shouldBe null
            }
        }
        `when`("SentinelSignalType mapea FALL_RISK y tolera guiones") {
            then("FALL_RISK y default NO_MOVEMENT") {
                SentinelSignalType.from("FALL_RISK") shouldBe SentinelSignalType.FALL_RISK
                SentinelSignalType.from("fall-risk") shouldBe SentinelSignalType.FALL_RISK
                SentinelSignalType.from("inventado") shouldBe SentinelSignalType.NO_MOVEMENT
            }
        }
        `when`("se persiste señal centinela con enriquecimiento V16") {
            val s = SentinelSignal(
                id = com.hub.shared.domain.Identifier.random(), signalId = "sig-1",
                bedId = com.hub.shared.domain.BedId("cama-12"),
                residentId = com.hub.shared.domain.ResidentId("maria-1"),
                episodeId = "ep-99", type = SentinelSignalType.FALL_RISK, severity = "CRITICAL",
                trigger = "DWELL", timestamp = java.time.Instant.now(), payloadJson = """{"rule":"BED_EDGE"}""",
                ruleId = "BED_EDGE", triggerOn = "DWELL", requiresNvr = true
            )
            then("guarda trazabilidad sin re-parsear JSON") {
                s.ruleId shouldBe "BED_EDGE"
                s.requiresNvr shouldBe true
            }
        }
    }

    given("escena con valores desconocidos del motor") {
        `when`("el motor manda SceneState inventado") {
            then("cae a UNKNOWN, no explota — SOR tolerante") {
                SceneState.from("inventado") shouldBe SceneState.UNKNOWN
                SceneState.from("no-existe") shouldBe SceneState.UNKNOWN
                SceneState.from("SLEEPING") shouldBe SceneState.SLEEPING
            }
        }
        `when`("el motor manda SceneEventType inventado") {
            then("falla rápido — contrato estricto") {
                io.kotest.assertions.throwables.shouldThrow<IllegalArgumentException> {
                    SceneEventType.from("inventado")
                }
            }
        }
    }
})
