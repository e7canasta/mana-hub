package com.hub.insights.integration

import com.hub.insights.domain.derive.Baseline
import com.hub.insights.domain.derive.SleepDerived
import com.hub.insights.domain.find.BathroomPolicy
import com.hub.insights.domain.find.CarePolicy
import com.hub.insights.domain.find.FindingCatalog
import com.hub.insights.domain.find.FindingContext
import com.hub.insights.domain.find.FindingPolicy
import com.hub.insights.domain.find.Polarity
import com.hub.insights.domain.find.SleepPolicy
import com.hub.insights.domain.recommend.WellbeingRecommendations
import com.hub.insights.engine.InsightContext
import com.hub.insights.engine.InsightEngine
import com.hub.insights.engine.SleepDayData
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.ZoneId

/**
 * Kotest 6.x BehaviorSpec — FindingPolicy: umbrales configurables por residente.
 *
 * Cada regla se puede prender/apagar individualmente por residente.
 * Si el residente no tiene política propia, se usa la default.
 */
class FindingPolicySpec : BehaviorSpec({

    val zone = ZoneId.of("America/Argentina/Buenos_Aires")

    val baseline = Baseline(
        admissionDate = LocalDate.of(2024, 1, 15),
        observedFrom = LocalDate.of(2026, 8, 1),
        observedDays = 30, ready = true,
    )

    fun ctx(
        restlessShare: Double = 0.30,
        careAvg: Double? = 15.0,
    ) = FindingContext(
        residentId = "jose", residentName = "José García",
        baseline = baseline,
        sleep = SleepDerived(
            avgCalmMinutes7d = 250, deltaCalmMinutesWoW = null,
            avgRestlessMinutes7d = 100, avgAsleepMinutes7d = 350,
            restlessShare = restlessShare, avgBedExits = null, maxBedExits = null,
            avgTimeInBedMinutes = null, sleepEfficiency = null,
            habitualFrom = null, habitualTo = null,
        ),
        sleepDays = emptyList(),
        bathroomDays = emptyList(),
        careAvgMinutes = careAvg,
        exitsLast7d = emptyList(),
        staffAfterExitCount = 0,
        riskLevel = "medium",
        bedEdgeWarningMinutes = null,
        zone = zone,
        windowDays = 14,
        relatedEpisodeIds = emptyList(),
    )

    // ════════════════════════════════════════════════════════════════════════
    // DEFAULTS — todos enabled por defecto
    // ════════════════════════════════════════════════════════════════════════

    given("una SleepPolicy con defaults") {
        val policy = SleepPolicy()

        `when`("se consultan los enabled") {
            then("restlessHighEnabled es true") {
                policy.restlessHighEnabled shouldBe true
            }
            then("exitsRisingEnabled es true") {
                policy.exitsRisingEnabled shouldBe true
            }
            then("dawnClusterEnabled es true") {
                policy.dawnClusterEnabled shouldBe true
            }
            then("sleepInRangeEnabled es true") {
                policy.sleepInRangeEnabled shouldBe true
            }
            then("dropWoWEnabled es true") {
                policy.dropWoWEnabled shouldBe true
            }
        }
    }

    given("una CarePolicy con defaults") {
        val policy = CarePolicy()

        `when`("se consulta careThinEnabled") {
            then("es true") {
                policy.careThinEnabled shouldBe true
            }
        }
    }

    given("una BathroomPolicy con defaults") {
        val policy = BathroomPolicy()

        `when`("se consulta bathroomNightEnabled") {
            then("es true") {
                policy.bathroomNightEnabled shouldBe true
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // APAGAR REGLAS — el director desactiva SLEEP_RESTLESS_HIGH
    // ════════════════════════════════════════════════════════════════════════

    given("una residente con restlessShare=0.30 y restlessHighEnabled=true") {
        val findings = FindingCatalog.evaluate(ctx(restlessShare = 0.30))

        `when`("se evalúa con defaults") {
            then("genera SLEEP_RESTLESS_HIGH") {
                findings.any { it.code == "SLEEP_RESTLESS_HIGH" } shouldBe true
            }
        }
    }

    given("una residente con restlessShare=0.30 y restlessHighEnabled=false") {
        val customPolicy = SleepPolicy(restlessHighEnabled = false)
        val findings = FindingCatalog.evaluate(ctx(restlessShare = 0.30), sleepPolicy = customPolicy)

        `when`("se evalúa con la regla apagada") {
            then("NO genera SLEEP_RESTLESS_HIGH") {
                findings.none { it.code == "SLEEP_RESTLESS_HIGH" } shouldBe true
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // APAGAR CARE_THIN — la cámara no cubre la habitación
    // ════════════════════════════════════════════════════════════════════════

    given("una residente con careAvg=15 y careThinEnabled=true") {
        val findings = FindingCatalog.evaluate(ctx(careAvg = 15.0))

        `when`("se evalúa con defaults") {
            then("genera CARE_THIN") {
                findings.any { it.code == "CARE_THIN" } shouldBe true
            }
        }
    }

    given("una residente con careAvg=15 y careThinEnabled=false") {
        val customPolicy = CarePolicy(careThinEnabled = false)
        val findings = FindingCatalog.evaluate(ctx(careAvg = 15.0), carePolicy = customPolicy)

        `when`("se evalúa con la regla apagada") {
            then("NO genera CARE_THIN") {
                findings.none { it.code == "CARE_THIN" } shouldBe true
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // CAMBIAR UMBRALES — el director ajusta el umbral de restless
    // ════════════════════════════════════════════════════════════════════════

    given("una residente con restlessShare=0.30 y umbral default (0.25)") {
        val findings = FindingCatalog.evaluate(ctx(restlessShare = 0.30))

        `when`("se evalúa con defaults") {
            then("genera SLEEP_RESTLESS_HIGH") {
                findings.any { it.code == "SLEEP_RESTLESS_HIGH" } shouldBe true
            }
        }
    }

    given("una residente con restlessShare=0.30 y umbral subido a 0.35") {
        val customPolicy = SleepPolicy(restlessHighThreshold = 0.35)
        val findings = FindingCatalog.evaluate(ctx(restlessShare = 0.30), sleepPolicy = customPolicy)

        `when`("se evalúa con umbral personalizado") {
            then("NO genera SLEEP_RESTLESS_HIGH (0.30 < 0.35)") {
                findings.none { it.code == "SLEEP_RESTLESS_HIGH" } shouldBe true
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // RECOMENDACIONES — enabled afecta recomendaciones
    // ════════════════════════════════════════════════════════════════════════

    given("una residente con restlessShare=0.22 y dropWoWEnabled=true") {
        val derived = SleepDerived(
            avgCalmMinutes7d = 250, deltaCalmMinutesWoW = -50,
            avgRestlessMinutes7d = 100, avgAsleepMinutes7d = 350,
            restlessShare = 0.22, avgBedExits = null, maxBedExits = null,
            avgTimeInBedMinutes = null, sleepEfficiency = null,
            habitualFrom = null, habitualTo = null,
        )

        `when`("se generan recomendaciones con defaults") {
            val recs = WellbeingRecommendations.forSleep(baseline, derived)
            then("genera SLEEP_DROP_WOW (-50 <= -45)") {
                recs.any { it.code == "SLEEP_DROP_WOW" } shouldBe true
            }
        }
    }

    given("una residente con restlessShare=0.22 y dropWoWEnabled=false") {
        val derived = SleepDerived(
            avgCalmMinutes7d = 250, deltaCalmMinutesWoW = -50,
            avgRestlessMinutes7d = 100, avgAsleepMinutes7d = 350,
            restlessShare = 0.22, avgBedExits = null, maxBedExits = null,
            avgTimeInBedMinutes = null, sleepEfficiency = null,
            habitualFrom = null, habitualTo = null,
        )
        val customPolicy = SleepPolicy(dropWoWEnabled = false)

        `when`("se generan recomendaciones con la regla apagada") {
            val recs = WellbeingRecommendations.forSleep(baseline, derived, customPolicy)
            then("NO genera SLEEP_DROP_WOW") {
                recs.none { it.code == "SLEEP_DROP_WOW" } shouldBe true
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // FINDING POLICY — compuesta con defaults heredados
    // ════════════════════════════════════════════════════════════════════════

    given("una FindingPolicy vacía") {
        val policy = FindingPolicy()

        `when`("se consultan las policies internas") {
            then("sleep usa defaults de SleepPolicy") {
                policy.sleep.restlessHighThreshold shouldBe 0.25
                policy.sleep.restlessHighEnabled shouldBe true
            }
            then("care usa defaults de CarePolicy") {
                policy.care.careThinMinutes shouldBe 20.0
                policy.care.careThinEnabled shouldBe true
            }
            then("bathroom usa defaults de BathroomPolicy") {
                policy.bathroom.nightMinAvg shouldBe 1.0
                policy.bathroom.bathroomNightEnabled shouldBe true
            }
        }
    }

    given("una FindingPolicy con sleep.restlessHighEnabled=false") {
        val policy = FindingPolicy(
            sleep = SleepPolicy(restlessHighEnabled = false),
        )

        `when`("se usa en FindingCatalog") {
            val findings = FindingCatalog.evaluate(
                ctx(restlessShare = 0.35),
                sleepPolicy = policy.sleep,
            )
            then("NO genera SLEEP_RESTLESS_HIGH (regla apagada)") {
                findings.none { it.code == "SLEEP_RESTLESS_HIGH" } shouldBe true
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // RESIDENTE vs DEFAULT — cascada
    // ════════════════════════════════════════════════════════════════════════

    given("una FindingPolicy default con restlessHighThreshold=0.25") {
        val defaultPolicy = FindingPolicy(isDefault = true)

        `when`("un residente no tiene política propia") {
            then("se usa la default") {
                defaultPolicy.sleep.restlessHighThreshold shouldBe 0.25
                defaultPolicy.sleep.restlessHighEnabled shouldBe true
            }
        }
    }

    given("una FindingPolicy de residente con restlessHighEnabled=false") {
        val residentPolicy = FindingPolicy(
            residentId = "jose",
            sleep = SleepPolicy(restlessHighEnabled = false),
        )

        `when`("se evalúa para ese residente") {
            then("la regla está apagada") {
                residentPolicy.sleep.restlessHighEnabled shouldBe false
            }
            then("el resto de las reglas siguen activas") {
                residentPolicy.sleep.exitsRisingEnabled shouldBe true
                residentPolicy.sleep.dawnClusterEnabled shouldBe true
            }
        }
    }
})
