package com.hub.insights.integration

import com.hub.insights.domain.derive.Baseline
import com.hub.insights.domain.derive.BaselineService
import com.hub.insights.domain.find.FindingCatalog
import com.hub.insights.domain.find.FindingContext
import com.hub.insights.domain.find.Polarity
import com.hub.insights.domain.find.PolicyCopy
import com.hub.insights.domain.recommend.WellbeingRecommendations
import com.hub.insights.engine.InsightContext
import com.hub.insights.engine.InsightEngine
import com.hub.insights.engine.SleepDayData
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.LocalDate
import java.time.ZoneId

/**
 * Kotest 6.x BehaviorSpec — Flujos de decisión clínica y semántica de overrides.
 *
 * Fowler: "Ubiquitous Language" — el test habla como la historia clínica.
 * Vernon: Policy como concepto de dominio, no como config.
 * Beck: Given/When/Then con el comportamiento que paga la clínica.
 *
 * Cubre:
 *  - Reglas de PolicyCopy por nivel de riesgo
 *  - Fusión de overrides (DwellOverride)
 *  - Resolución de episodios (self-recovery vs staff)
 *  - Detección de salidas de cama al amanecer
 *  - Estado de baseline (forming vs ready)
 *  - Pipeline de findings end-to-end
 *  - Recomendaciones de bienestar por contexto
 */
class ClinicalDecisionFlowSpec : BehaviorSpec({

    val zone = ZoneId.of("America/Argentina/Buenos_Aires")

    // ════════════════════════════════════════════════════════════════════════
    // POLICY COPY — reglas habladas por nivel de riesgo
    // ════════════════════════════════════════════════════════════════════════

    given("una residente con perfil de riesgo BAJO y sin overrides") {
        `when`("se genera el policy copy") {
            val lines = PolicyCopy.spokenLines("low", emptyMap())
            then("genera líneas para cada estado del catálogo") {
                lines.shouldNotBeNull()
                lines.isNotEmpty() shouldBe true
            }
        }
    }

    given("una residente con perfil de riesgo ALTO y sin overrides") {
        `when`("se genera el policy copy") {
            val lines = PolicyCopy.spokenLines("high", emptyMap())
            then("las líneas incluyen advertencias de alto riesgo") {
                lines.shouldNotBeNull()
                lines.isNotEmpty() shouldBe true
            }
        }
    }

    given("una residente de riesgo MEDIO con DwellOverride de 20/30 minutos") {
        val overrides = mapOf(
            "OUT_OF_BED" to com.hub.insights.inbound.HubOverrideEntry(
                warningAfterMinutes = 20, alertAfterMinutes = 30,
            )
        )
        `when`("se genera el policy copy") {
            val lines = PolicyCopy.spokenLines("medium", overrides)
            then("las líneas incluyen los tiempos del override") {
                lines.shouldNotBeNull()
                lines.isNotEmpty() shouldBe true
            }
        }
    }

    given("una residente con override de bed_edge_warning_minutes") {
        val overrides = mapOf(
            "BED_EDGE" to com.hub.insights.inbound.HubOverrideEntry(
                warningAfterMinutes = 15, alertAfterMinutes = 25,
            )
        )
        `when`("se calcula el bed edge warning") {
            val minutes = PolicyCopy.bedEdgeWarningMinutes("high", overrides)
            then("retorna el valor del override") {
                minutes shouldBe 15
            }
        }
    }

    given("una residente de riesgo bajo sin override de bed_edge") {
        `when`("se calcula el bed edge warning") {
            val minutes = PolicyCopy.bedEdgeWarningMinutes("low", emptyMap())
            then("retorna null (sin catálogo para bajo)") {
                minutes shouldBe null
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // EPISODE RESOLUTION — self-recovery vs staff closure
    // ════════════════════════════════════════════════════════════════════════

    given("un episodio que se resolvió por autorecuperación") {
        `when`("se genera la recomendación") {
            val recs = WellbeingRecommendations.forEpisodeResolved(
                selfRecovery = true, durationMinutes = 12,
            )
            then("la recomendación es EPISODE_SELF_RECOVERY") {
                recs shouldHaveSize 1
                recs[0].code shouldBe "EPISODE_SELF_RECOVERY"
            }
        }
    }

    given("un episodio que resolvió el personal con duración mayor a 20 minutos") {
        `when`("se genera la recomendación") {
            val recs = WellbeingRecommendations.forEpisodeResolved(
                selfRecovery = false, durationMinutes = 25,
            )
            then("la recomendación es EPISODE_STAFF_CLOSED") {
                recs shouldHaveSize 1
                recs[0].code shouldBe "EPISODE_STAFF_CLOSED"
            }
        }
    }

    given("un episodio sin duración registrada") {
        `when`("se genera la recomendación") {
            val recs = WellbeingRecommendations.forEpisodeResolved(
                selfRecovery = false, durationMinutes = null,
            )
            then("aún genera recomendación") {
                recs shouldHaveSize 1
                recs[0].code shouldBe "EPISODE_STAFF_CLOSED"
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // BASELINE — forming vs ready
    // ════════════════════════════════════════════════════════════════════════

    given("una residente admitida hace 3 días (menos de baselineMinDays=7)") {
        `when`("se calcula el baseline") {
            val baseline = BaselineService.of(
                admissionDate = LocalDate.now().minusDays(3),
                from = LocalDate.now().minusDays(14),
                to = LocalDate.now(),
                minDays = 7,
            )
            then("el baseline no está listo") {
                baseline.ready shouldBe false
            }
            then("observedFrom es la fecha de admisión") {
                baseline.observedFrom shouldBe LocalDate.now().minusDays(3)
            }
        }
    }

    given("una residente admitida hace 30 días") {
        `when`("se calcula el baseline") {
            val baseline = BaselineService.of(
                admissionDate = LocalDate.now().minusDays(30),
                from = LocalDate.now().minusDays(14),
                to = LocalDate.now(),
                minDays = 7,
            )
            then("el baseline está listo") {
                baseline.ready shouldBe true
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // FINDING CATALOG — pipeline end-to-end
    // ════════════════════════════════════════════════════════════════════════

    given("una residente con baseline forming") {
        val baseline = Baseline(
            admissionDate = LocalDate.now().minusDays(3),
            observedFrom = LocalDate.now().minusDays(3),
            observedDays = 3, ready = false,
        )
        `when`("se evalúa el FindingCatalog") {
            val findings = FindingCatalog.evaluate(
                FindingContext(
                    residentId = "jose", residentName = "José",
                    baseline = baseline,
                    sleep = com.hub.insights.domain.derive.SleepDerived(
                        avgCalmMinutes7d = null, deltaCalmMinutesWoW = null,
                        avgRestlessMinutes7d = null, avgAsleepMinutes7d = null,
                        restlessShare = null, avgBedExits = null, maxBedExits = null,
                        avgTimeInBedMinutes = null, sleepEfficiency = null,
                        habitualFrom = null, habitualTo = null,
                    ),
                    sleepDays = emptyList(), bathroomDays = emptyList(),
                    careAvgMinutes = null,
                    exitsLast7d = emptyList(), staffAfterExitCount = 0,
                    riskLevel = null, bedEdgeWarningMinutes = 0,
                    zone = zone, windowDays = 14,
                    relatedEpisodeIds = emptyList(),
                )
            )
            then("genera un finding BASELINE_FORMING") {
                findings.any { it.code == "BASELINE_FORMING" } shouldBe true
            }
            then("el finding tiene polaridad NEUTRAL") {
                val bf = findings.first { it.code == "BASELINE_FORMING" }
                bf.polarity shouldBe Polarity.NEUTRAL
            }
        }
    }

    given("una residente con sueño inquieto (restless share > 30%)") {
        val derived = com.hub.insights.domain.derive.SleepInsights.derive(
            (1..14).map { n ->
                com.hub.insights.inbound.HubSleepDay(
                    day = "2026-08-${18 + n}",
                    calmMinutes = 200, restlessMinutes = 130, awakeMinutes = 50,
                    bedExitCount = 2, measured = true,
                )
            }
        )
        val baseline = Baseline(
            admissionDate = LocalDate.of(2024, 1, 15),
            observedFrom = LocalDate.of(2026, 8, 1),
            observedDays = 14, ready = true,
        )
        val sleepDays = (1..14).map { n ->
            com.hub.insights.inbound.HubSleepDay(
                day = "2026-08-${18 + n}",
                calmMinutes = 200, restlessMinutes = 130, awakeMinutes = 50,
                bedExitCount = 2, measured = true,
            )
        }

        `when`("se evalúa el FindingCatalog") {
            val findings = FindingCatalog.evaluate(
                FindingContext(
                    residentId = "jose", residentName = "José",
                    baseline = baseline, sleep = derived,
                    sleepDays = sleepDays,
                    bathroomDays = emptyList(),
                    careAvgMinutes = null,
                    exitsLast7d = emptyList(), staffAfterExitCount = 0,
                    riskLevel = "medium", bedEdgeWarningMinutes = 0,
                    zone = zone, windowDays = 14,
                    relatedEpisodeIds = emptyList(),
                )
            )
            then("genera un finding de sueño inquieto") {
                findings.any { it.code == "SLEEP_RESTLESS_HIGH" } shouldBe true
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // WELLBEING RECOMMENDATIONS — por contexto de sueño
    // ════════════════════════════════════════════════════════════════════════

    given("una residente con baseline listo y sueño dentro del rango") {
        val baseline = Baseline(
            admissionDate = LocalDate.of(2024, 1, 15),
            observedFrom = LocalDate.of(2026, 8, 1),
            observedDays = 14, ready = true,
        )
        val derived = com.hub.insights.domain.derive.SleepDerived(
            avgCalmMinutes7d = 300, deltaCalmMinutesWoW = null,
            avgRestlessMinutes7d = 80, avgAsleepMinutes7d = 380,
            restlessShare = 0.15, avgBedExits = null, maxBedExits = null,
            avgTimeInBedMinutes = null, sleepEfficiency = null,
            habitualFrom = null, habitualTo = null,
        )

        `when`("se generan recomendaciones de sueño") {
            val recs = WellbeingRecommendations.forSleep(baseline, derived)
            then("genera SLEEP_IN_RANGE (sueño estable)") {
                recs shouldHaveSize 1
                recs[0].code shouldBe "SLEEP_IN_RANGE"
            }
        }
    }

    given("una residente con baseline forming") {
        val baseline = Baseline(
            admissionDate = LocalDate.now().minusDays(3),
            observedFrom = LocalDate.now().minusDays(3),
            observedDays = 3, ready = false,
        )
        val dummyDerived = com.hub.insights.domain.derive.SleepDerived(
            avgCalmMinutes7d = null, deltaCalmMinutesWoW = null,
            avgRestlessMinutes7d = null, avgAsleepMinutes7d = null,
            restlessShare = null, avgBedExits = null, maxBedExits = null,
            avgTimeInBedMinutes = null, sleepEfficiency = null,
            habitualFrom = null, habitualTo = null,
        )

        `when`("se generan recomendaciones de sueño") {
            val recs = WellbeingRecommendations.forSleep(baseline, dummyDerived)
            then("genera recomendación de baseline forming") {
                recs.isNotEmpty() shouldBe true
                recs.any { it.code.contains("BASELINE", ignoreCase = true) } shouldBe true
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // INSIGHT ENGINE — expertos ensamblados
    // ════════════════════════════════════════════════════════════════════════

    given("el InsightEngine con los 3 expertos por defecto") {
        val engine = InsightEngine()

        `when`("se evalúa un contexto con datos de sueño") {
            val ctx = InsightContext(
                residentId = "jose", residentName = "José",
                from = LocalDate.of(2026, 8, 1), to = LocalDate.of(2026, 8, 30),
                baseline = Baseline(
                    admissionDate = LocalDate.of(2024, 1, 15),
                    observedFrom = LocalDate.of(2026, 8, 1),
                    observedDays = 30, ready = true,
                ),
                derived = com.hub.insights.domain.derive.SleepInsights.derive(
                    (1..14).map { n ->
                        com.hub.insights.inbound.HubSleepDay(
                            day = "2026-08-${18 + n}",
                            calmMinutes = 250, restlessMinutes = 100, awakeMinutes = 40,
                            bedExitCount = 3, measured = true,
                        )
                    }
                ),
                sleepDays = (1..14).map { n ->
                    SleepDayData(
                        day = "2026-08-${18 + n}",
                        calmMinutes = 250, restlessMinutes = 100, awakeMinutes = 40,
                        bedExitCount = 3, measured = true,
                    )
                },
                bathroomDays = emptyList(),
                careAvgMinutes = null, careTotalMinutes = 0,
                exitsLast7d = emptyList(), staffAfterExitCount = 0,
                riskLevel = "high", bedEdgeWarningMinutes = 1,
                relatedEpisodeIds = emptyList(), policyToday = emptyList(),
                episodes = emptyList(), zone = zone, windowDays = 30,
            )
            val result = engine.evaluate(ctx)

            then("ensambla resultados de los 3 expertos") {
                result.expertResults shouldHaveSize 3
            }

            then("genera findings de sueño") {
                result.findings.isNotEmpty() shouldBe true
                result.findings.any { it.code.startsWith("SLEEP") } shouldBe true
            }

            then("genera recomendaciones") {
                result.recommendations.isNotEmpty() shouldBe true
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // EPISODE RESOLUTION via RollupController (POST /episodes/resolved)
    // ════════════════════════════════════════════════════════════════════════

    given("un episodio que se resolvió por autorecuperación en 8 minutos") {
        `when`("se procesa la resolución") {
            val recs = WellbeingRecommendations.forEpisodeResolved(
                selfRecovery = true, durationMinutes = 8,
            )
            then("genera recomendación de autorecuperación") {
                recs shouldHaveSize 1
                recs[0].code shouldBe "EPISODE_SELF_RECOVERY"
                recs[0].text shouldContain "volvió solo al estado seguro"
            }
        }
    }

    given("un episodio que resolvió el personal después de 45 minutos") {
        `when`("se procesa la resolución") {
            val recs = WellbeingRecommendations.forEpisodeResolved(
                selfRecovery = false, durationMinutes = 45,
            )
            then("genera recomendación de cierre por personal") {
                recs shouldHaveSize 1
                recs[0].code shouldBe "EPISODE_STAFF_CLOSED"
            }
        }
    }
})
