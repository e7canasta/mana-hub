package com.hub.policy.specs

import com.hub.policy.application.service.AlarmProfileResponseBuilder
import com.hub.policy.domain.model.*
import com.hub.shared.domain.ResidentId
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey

/**
 * Kotest 6.x BehaviorSpec — Flujo de decisión clínica + semántica de overrides.
 *
 * No testea `data class` ni `from()`. Testea lo que el director decide y paga:
 *  - ¿Qué nivel le corresponde? (riesgo → vigilancia)
 *  - ¿Qué ve en el panel como regla efectiva?
 *  - ¿Qué pasa cuando ajusta un tiempo sin tocar gravedad?
 *  - ¿Silenciar sin borrar (observeOnly)?
 *
 * Fowler: test de comportamiento, no de dato. Vernon: Aggregate protege invariante.
 */
class ClinicalDecisionBehaviorSpec : BehaviorSpec({

    val builder = AlarmProfileResponseBuilder()

    // ── Flujo: riesgo → nivel → regla efectiva → recomendación ────────────────

    given("una residente nueva sin plantilla, riesgo BAJO") {
        val perfil = AlarmProfileVersion.create(ResidentId("maria-1"), "dra.garcia")
            .update(MobilityAid.NONE, false, PolicyMode.PRESET, null, RiskLevel.LOW, "dra.garcia")
        val catalogo = DagCatalogs.forLevel(builder.resolveWatchLevel(perfil.riskLevel, perfil.templateId))

        `when`("el director consulta el panel") {
            val resp = builder.buildResponse(perfil, "maria-1", WatchLevel.STANDARD, catalogo, emptyMap())
            then("ve STANDARD sin reglas que alerten — solo observa") {
                resp.effective.level shouldBe "standard"
                resp.effective.rules shouldNotContainKey "lying"
                resp.effective.rules.isEmpty() shouldBe true
                resp.recommendation.changed shouldBe false
                resp.recommendation.score shouldBe 20
            }
        }
    }

    given("una residente riesgo ALTO sin plantilla") {
        val perfil = AlarmProfileVersion.create(ResidentId("maria-1"), "dra.garcia")
            .update(MobilityAid.NONE, false, PolicyMode.PRESET, null, RiskLevel.HIGH, "dra.garcia")
        val nivel = builder.resolveWatchLevel(perfil.riskLevel, perfil.templateId)
        val catalogo = DagCatalogs.forLevel(nivel)

        `when`("se construye el panel efectivo") {
            val resp = builder.buildResponse(perfil, "maria-1", nivel, catalogo, emptyMap())
            then("ve FALL_RISK con borde a 2 min — avisa rápido") {
                nivel shouldBe WatchLevel.FALL_RISK
                resp.effective.rules shouldContainKey "bed_edge"
                resp.effective.rules["bed_edge"]!!.params!!["alertAfter"] shouldBe 2L
                resp.recommendation.level shouldBe "fall_risk"
            }
        }
        `when`("aún está en STANDARD por plantilla vieja") {
            val respViejo = builder.buildResponse(perfil, "maria-1", WatchLevel.STANDARD, DagCatalogs.STANDARD, emptyMap())
            then("la recomendación marca changed=true y sugiere FALL_RISK") {
                respViejo.recommendation.changed shouldBe true
                respViejo.recommendation.level shouldBe "fall_risk"
                respViejo.recommendation.score shouldBe 80
            }
        }
    }

    given("residente MEDIO con plantilla explícita fall_risk") {
        `when`("el director dejó escrito fall_risk a mano") {
            val nivel = builder.resolveWatchLevel(RiskLevel.MEDIUM, TemplateId.from("fall_risk"))
            then("manda la plantilla, no el riesgo") {
                nivel shouldBe WatchLevel.FALL_RISK
            }
        }
        `when`("la plantilla es basura 'inexistente'") {
            val nivel = builder.resolveWatchLevel(RiskLevel.HIGH, TemplateId.from("inexistente"))
            then("cae al fallback por riesgo → FALL_RISK, no explota") {
                nivel shouldBe WatchLevel.FALL_RISK
            }
        }
    }

    // ── Semántica de override: null = siga catálogo, valor = director habló ───

    given("catálogo FALL_RISK con BED_EDGE WARNING") {
        val catalogo = DagCatalogs.forLevel(WatchLevel.FALL_RISK)
        val regla = catalogo.residentStates[StateKind.BED_EDGE]!!
        `when`("el director ajusta solo alertAfter a 1 sin mencionar severity") {
            val ajuste = PolicyOverride.DwellOverride(
                com.hub.shared.domain.Identifier.random(), "BED_EDGE", "bed_edge",
                warningAfterMinutes = 1, alertAfterMinutes = 1, severity = null
            )
            then("severity queda null — el builder usa la del catálogo, no degrada") {
                regla.severity shouldBe Severity.WARNING
                ajuste.severity shouldBe null
                ajuste.warningAfterMinutes shouldBe 1
            }
        }
        `when`("el director explicita CRITICAL + STAFF_AND_SAFE") {
            val ajuste = PolicyOverride.DwellOverride(
                com.hub.shared.domain.Identifier.random(), "BED_EDGE", "bed_edge", 1, 2,
                severity = "CRITICAL", closureCondition = "STAFF_AND_SAFE"
            )
            then("se respeta lo escrito") {
                ajuste.severity shouldBe "CRITICAL"
                ajuste.closureCondition shouldBe "STAFF_AND_SAFE"
            }
        }
    }

    given("el director quiere silenciar sin borrar") {
        `when`("marca observeOnly=true en una permanencia") {
            val silenciada = PolicyOverride.DwellOverride(
                com.hub.shared.domain.Identifier.random(), "BED_EDGE", "bed_edge", 1, 2, observeOnly = true
            )
            val sinMencion = PolicyOverride.DwellOverride(
                com.hub.shared.domain.Identifier.random(), "STANDING", "standing", null, null, observeOnly = null
            )
            then("true = calle, null = siga catálogo — no es lo mismo") {
                silenciada.observeOnly shouldBe true
                silenciada.alertAfterMinutes shouldBe 2 // el tiempo sigue, pero no suena
                sinMencion.observeOnly shouldBe null
            }
        }
        `when`("silencia una transición") {
            val t = PolicyOverride.HysteresisOverride(
                com.hub.shared.domain.Identifier.random(), "LYING->STANDING", "LYING->STANDING", 5, observeOnly = true
            )
            then("también se puede silenciar la histéresis") {
                t.observeOnly shouldBe true
                t.hysteresisSeconds shouldBe 5
            }
        }
        `when`("silencia un comeback") {
            val c = PolicyOverride.ComeBackOverride(
                com.hub.shared.domain.Identifier.random(), "LYING", "lying", 1, 2, observeOnly = true
            )
            then("las tres variantes toleran observeOnly") {
                c.observeOnly shouldBe true
            }
        }
    }

    given("el panel manda hysteresis con transición LYING->STANDING") {
        `when`("el director ajusta histéresis a 3s") {
            val h = PolicyOverride.HysteresisOverride(
                com.hub.shared.domain.Identifier.random(), "LYING->STANDING", "LYING->STANDING", 3,
                severity = "HIGH", closureCondition = "STAFF_ONLY"
            )
            then("la transición puede llevar gravedad y cierre — asimetría corregida") {
                h.hysteresisSeconds shouldBe 3
                h.severity shouldBe "HIGH"
                h.closureCondition shouldBe "STAFF_ONLY"
            }
        }
    }

    // ── Traits para el panel ─────────────────────────────────────────────────

    given("residente HIGH + silla de ruedas + autopiloto") {
        val perfil = AlarmProfileVersion.create(ResidentId("maria-1"), "x")
            .update(MobilityAid.WHEELCHAIR, true, PolicyMode.PRESET, null, RiskLevel.HIGH, "x")
        `when`("se resuelven traits") {
            val traits = builder.resolveTraits(perfil)
            then("el panel muestra fall_risk + wheelchair_user + autopilot") {
                traits shouldContain "fall_risk"
                traits shouldContain "wheelchair_user"
                traits shouldContain "autopilot"
            }
        }
    }
})
