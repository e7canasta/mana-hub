package com.hub.policy.specs

import com.hub.policy.application.service.AlarmProfileResponseBuilder
import com.hub.policy.domain.model.*
import com.hub.shared.domain.ResidentId
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.collections.shouldBeEmpty

/**
 * Kotest BehaviorSpec — Diseño de regla efectiva: el catálogo + perfil → lo que el motor evalúa.
 *
 * Fowler: la decisión de qué regla alerta vive en el dominio, no en el DTO del panel.
 * Vernon: el catálogo es un Value Object inmutable — la regla efectiva es una proyección, no un Aggregate nuevo.
 * Diseño: si mañana añaden WatchLevel CRITICAL con alertAfter 1m, este spec debe fallar hasta que la clínica lo valide.
 */
class EffectiveRuleBehaviorSpec : BehaviorSpec({

    val builder = AlarmProfileResponseBuilder()

    given("catálogo STANDARD — baseline, solo observa") {
        val catalogo = DagCatalogs.STANDARD
        `when`("construyo regla efectiva para riesgo LOW") {
            val perfil = AlarmProfileVersion.create(ResidentId("maria-1"), "x")
                .update(MobilityAid.NONE, false, PolicyMode.PRESET, null, RiskLevel.LOW, "x")
            val resp = builder.buildResponse(perfil, "maria-1", WatchLevel.STANDARD, catalogo, emptyMap())
            then("no hay reglas que alerten — panel vacío, diseño lo exige") {
                resp.effective.rules.isEmpty() shouldBe true
                resp.effective.level shouldBe "standard"
                catalogo.residentStates.values.none { it.alerts } shouldBe true
            }
        }
    }

    given("catálogo FALL_RISK vs NIGHT_WANDERING — misma cama, distinto umbral clínico") {
        val fall = DagCatalogs.FALL_RISK
        val night = DagCatalogs.NIGHT_WANDERING
        `when`("comparo BED_EDGE") {
            then("FALL_RISK avisa en 2m, NIGHT en 5m — diseño clínico, no casual") {
                fall.residentStates[StateKind.BED_EDGE]!!.alertAfter!!.toMinutes() shouldBe 2
                night.residentStates[StateKind.BED_EDGE]!!.alertAfter!!.toMinutes() shouldBe 5
                // El builder solo expone las que alertan — LYING nunca alerta
                val perfilHigh = AlarmProfileVersion.create(ResidentId("x"), "x")
                    .update(MobilityAid.NONE, false, PolicyMode.PRESET, null, RiskLevel.HIGH, "x")
                val effectiveFall = builder.buildResponse(perfilHigh, "x", WatchLevel.FALL_RISK, fall, emptyMap()).effective
                effectiveFall.rules shouldContainKey "bed_edge"
                effectiveFall.rules shouldNotContainKey "lying"
                effectiveFall.rules["bed_edge"]!!.params!!["closure"] shouldBe "STAFF_OR_SAFE"
                effectiveFall.rules["standing"]!!.params!!["closure"] shouldBe "SAFE_ONLY"
            }
        }
        `when`("CRITICAL cierra solo con staff y seguridad") {
            val crit = DagCatalogs.CRITICAL
            then("closure STAFF_AND_SAFE — diseño más restrictivo que WARNING") {
                crit.residentStates[StateKind.BED_EDGE]!!.closureCondition shouldBe ClosureCondition.STAFF_AND_SAFE
                crit.residentStates[StateKind.BED_EDGE]!!.severity shouldBe Severity.CRITICAL
            }
        }
    }

    given("recomendación que el panel sugiere") {
        `when`("HIGH aún en STANDARD") {
            val perfil = AlarmProfileVersion.create(ResidentId("x"), "x")
                .update(MobilityAid.NONE, false, PolicyMode.PRESET, null, RiskLevel.HIGH, "x")
            val resp = builder.buildResponse(perfil, "x", WatchLevel.STANDARD, DagCatalogs.STANDARD, emptyMap())
            then("sugiere FALL_RISK, changed=true, score 80") {
                resp.recommendation.level shouldBe "fall_risk"
                resp.recommendation.changed shouldBe true
                resp.recommendation.score shouldBe 80
                resp.recommendation.suggestedTemplate shouldBe "fall_risk"
            }
        }
        `when`("ya coincide MEDIUM→NIGHT_WANDERING") {
            val perfil = AlarmProfileVersion.create(ResidentId("x"), "x")
                .update(MobilityAid.NONE, false, PolicyMode.PRESET, null, RiskLevel.MEDIUM, "x")
            val resp = builder.buildResponse(perfil, "x", WatchLevel.NIGHT_WANDERING, DagCatalogs.NIGHT_WANDERING, emptyMap())
            then("no sugiere cambio") {
                resp.recommendation.changed shouldBe false
                resp.recommendation.score shouldBe 50
            }
        }
    }

    given("transiciones con hysteresis — cuánto creerle al sensor") {
        `when`("STANDARD tiene 15 transiciones, FALL_RISK solo 3") {
            then("STANDARD observa más — diseño de vigilancia, no bug") {
                DagCatalogs.STANDARD.transitions.size shouldBe 15
                DagCatalogs.FALL_RISK.transitions.size shouldBe 3
                DagCatalogs.FALL_RISK.transitions.any { it.recordBefore != null } shouldBe true // LYING→STANDING con ventana de grabación
            }
        }
    }
})
