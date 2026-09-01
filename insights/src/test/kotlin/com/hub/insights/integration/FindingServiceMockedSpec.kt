package com.hub.insights.integration

import com.hub.care.domain.model.CareSummary
import com.hub.care.domain.repository.CareSummaryRepository
import com.hub.history.domain.model.EventSource
import com.hub.history.domain.model.EpisodeKind
import com.hub.history.domain.model.HistoryEpisode
import com.hub.history.domain.model.HistoryEpisodeSeverity
import com.hub.history.domain.repository.HistoryEpisodeDetectionRepository
import com.hub.insights.application.FindingService
import com.hub.insights.config.InsightsProperties
import com.hub.observation.domain.model.BathroomSummary
import com.hub.observation.domain.model.SceneEvent
import com.hub.observation.domain.model.SceneEventType
import com.hub.observation.domain.model.SceneState
import com.hub.observation.domain.model.SleepSummary
import com.hub.observation.domain.model.TriggerType
import com.hub.observation.domain.repository.SceneEventRepository
import com.hub.observation.domain.repository.SummaryRepository
import com.hub.policy.domain.model.AlarmProfileId
import com.hub.policy.domain.model.AlarmProfileVersion
import com.hub.policy.domain.model.PolicyOverride
import com.hub.policy.domain.model.RiskLevel
import com.hub.policy.domain.repository.AlarmProfileOverrideRepository
import com.hub.policy.domain.repository.AlarmProfileRepository
import com.hub.population.domain.model.Resident
import com.hub.population.domain.repository.ResidentRepository
import com.hub.shared.domain.BedId
import com.hub.shared.domain.Identifier
import com.hub.shared.domain.ResidentId
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Kotest 6.x BehaviorSpec — FindingService con repos mockeados.
 *
 * Fowler: "Ubiquitous Language" — el test habla como la historia clínica.
 * Vernon: Aggregate + Domain Event, no anemic Map.
 * Beck: Given/When/Then con el comportamiento que paga.
 *
 * Cubre los flujos deFindingService que antes dependían de HubClient
 * y ahora usan repos directos del SOR.
 */
class FindingServiceMockedSpec : BehaviorSpec({

    val zone = ZoneId.of("America/Argentina/Buenos_Aires")
    val properties = InsightsProperties(
        hubUrl = "http://localhost:1",
        timezone = "America/Argentina/Buenos_Aires",
        baselineMinDays = 7,
    )

    // ── Fixtures de dominio ────────────────────────────────────────────────

    val joseId = ResidentId.from("jose")
    val jose = Resident.reconstitute(
        id = joseId, externalId = "ext-jose", fullName = "José García",
        birthDate = LocalDate.of(1940, 3, 15), admissionDate = LocalDate.of(2024, 1, 15),
        status = com.hub.population.domain.model.ResidentStatus.ACTIVE,
        dischargedAt = null, dischargedBy = null, version = 0,
    )

    val mariaId = ResidentId.from("maria")
    val maria = Resident.reconstitute(
        id = mariaId, externalId = "ext-maria", fullName = "María López",
        birthDate = LocalDate.of(1938, 7, 22), admissionDate = LocalDate.of(2025, 6, 1),
        status = com.hub.population.domain.model.ResidentStatus.ACTIVE,
        dischargedAt = null, dischargedBy = null, version = 0,
    )

    val bedId = BedId.from("bed-001")

    fun aSleepSummary(
        residentId: ResidentId,
        date: LocalDate,
        calm: Int = 300, restless: Int = 80, awake: Int = 40,
        outOfBed: Int = 15, exits: Int = 2, wakes: Int = 3,
    ) = SleepSummary.create(
        sourceRecordId = "sr-${date}", residentId = residentId, observedOn = date,
        calmMinutes = calm, restlessMinutes = restless, awakeMinutes = awake,
        outOfBedMinutes = outOfBed, bedExitCount = exits, wakeCount = wakes,
        source = "rollup", modelVersion = "v1", confidence = 0.95,
    )

    fun aBathroomSummary(
        residentId: ResidentId, date: LocalDate,
        visits: Int = 3, nightVisits: Int = 1, assisted: Int = 0, minutes: Int = 12,
    ) = BathroomSummary.create(
        sourceRecordId = "br-${date}", residentId = residentId, observedOn = date,
        visitCount = visits, nightVisitCount = nightVisits, assistedCount = assisted,
        totalMinutes = minutes, source = "rollup", modelVersion = "v1", confidence = 0.9,
    )

    fun aCareSummary(
        residentId: ResidentId, date: LocalDate,
        totalMinutes: Int = 20, proactiveMinutes: Int = 15,
    ) = CareSummary.create(
        sourceRecordId = "cr-${date}", residentId = residentId, observedOn = date,
        totalMinutes = totalMinutes, proactiveMinutes = proactiveMinutes,
        roundsCount = 2, notesCount = 1, source = "rollup",
    )

    fun anEpisode(
        residentId: ResidentId, occurredAt: Instant,
        kind: EpisodeKind = EpisodeKind.FALL,
        severity: HistoryEpisodeSeverity = HistoryEpisodeSeverity.WARNING,
        selfRecovery: Boolean = false,
    ) = HistoryEpisode.reconstitute(
        com.hub.history.domain.model.HistoryEpisodeData(
            id = com.hub.history.domain.model.HistoryEpisodeId.random(),
            sourceRecordId = "ep-${occurredAt.epochSecond}",
            residentId = residentId, bedId = bedId, sourceAlertId = null,
            kind = kind, severity = severity, occurredAt = occurredAt,
            activity = null, injuryStatus = null, selfRecovery = selfRecovery,
            responseSeconds = null, narrative = null, source = EventSource.CAMERA,
            modelVersion = null, confidence = null, provenanceJson = "{}", version = 0,
        )
    )

    fun aProfile(
        residentId: ResidentId,
        riskLevel: RiskLevel = RiskLevel.MEDIUM,
    ) = AlarmProfileVersion.reconstitute(
        id = AlarmProfileId.from("profile-${residentId.value}"),
        residentId = residentId, validFrom = Instant.now(), validTo = null,
        mobilityAid = null, autopilot = false, mode = null, templateId = null,
        catalogVersion = null, updatedBy = "test", riskLevel = riskLevel, version = 0,
    )

    fun aSceneEvent(
        residentId: ResidentId, timestamp: Instant,
        from: SceneState = SceneState.SLEEPING, to: SceneState = SceneState.EMPTY,
    ) = SceneEvent(
        id = Identifier.random(), eventId = "se-${timestamp.epochSecond}",
        bedId = bedId, residentId = residentId,
        eventType = SceneEventType.STATE_CHANGED,
        fromState = from, toState = to, triggerType = TriggerType.EVENT_DRIVEN,
        timestamp = timestamp, payloadJson = "{}",
    )

    fun mockRepos(
        residentRepo: ResidentRepository = mockk(),
        summaryRepo: SummaryRepository = mockk(),
        sceneEventRepo: SceneEventRepository = mockk(),
        careSummaryRepo: CareSummaryRepository = mockk(),
        historyRepo: HistoryEpisodeDetectionRepository = mockk(),
        alarmProfileRepo: AlarmProfileRepository = mockk(),
        alarmOverrideRepo: AlarmProfileOverrideRepository = mockk(),
    ) = Triple(
        residentRepo to summaryRepo,
        sceneEventRepo to careSummaryRepo,
        historyRepo to alarmProfileRepo to alarmOverrideRepo,
    )

    // ── Historia 1: residente no existe ────────────────────────────────────
    given("una residente que no existe en el sistema") {
        val repo = mockk<ResidentRepository>()
        every { repo.findById(any()) } returns null

        val service = FindingService(
            residentRepository = repo,
            summaryRepository = mockk(relaxed = true),
            sceneEventRepository = mockk(relaxed = true),
            careSummaryRepository = mockk(relaxed = true),
            historyEpisodeRepository = mockk(relaxed = true),
            alarmProfileRepository = mockk(relaxed = true),
            alarmOverrideRepository = mockk(relaxed = true),
            findingPolicyService = mockk(relaxed = true),
            properties = properties,
        )

        `when`("se pide el briefing de esa residente") {
            val result = service.residentBriefing("no-existe", LocalDate.now().minusDays(14), LocalDate.now())
            then("retorna null") {
                result shouldBe null
            }
        }
    }

    // ── Historia 2: residente existe pero sin datos ────────────────────────
    given("una residente activa recién admitida sin datos de sueño en el período") {
        val joseRecent = Resident.reconstitute(
            id = joseId, externalId = "ext-jose", fullName = "José García",
            birthDate = LocalDate.of(1940, 3, 15),
            admissionDate = LocalDate.now().minusDays(3),
            status = com.hub.population.domain.model.ResidentStatus.ACTIVE,
            dischargedAt = null, dischargedBy = null, version = 0,
        )
        val summaryRepo = mockk<SummaryRepository>()
        every { summaryRepo.findSleepByResidentAndRange(any(), any(), any()) } returns emptyList()
        every { summaryRepo.findBathroomByResidentAndRange(any(), any(), any()) } returns emptyList()

        val service = FindingService(
            residentRepository = mockk<ResidentRepository> {
                every { findById(joseId) } returns joseRecent
            },
            summaryRepository = summaryRepo,
            sceneEventRepository = mockk<SceneEventRepository> {
                every { findByResidentId(any(), any<Instant>(), any<Instant>()) } returns emptyList()
            },
            careSummaryRepository = mockk<CareSummaryRepository> {
                every { findByResidentAndRange(any(), any(), any()) } returns emptyList()
            },
            historyEpisodeRepository = mockk<HistoryEpisodeDetectionRepository> {
                every { findByResidentId(any()) } returns emptyList()
            },
            alarmProfileRepository = mockk<AlarmProfileRepository> {
                every { findCurrentByResidentId(any()) } returns null
            },
            alarmOverrideRepository = mockk<AlarmProfileOverrideRepository>(),
            findingPolicyService = mockk(relaxed = true),
            properties = properties,
        )

        `when`("se pide el briefing") {
            val from = LocalDate.of(2026, 8, 1)
            val to = LocalDate.of(2026, 8, 30)
            val result = service.residentBriefing("jose", from, to)

            then("retorna un briefing con baselineForming") {
                result.shouldNotBeNull()
                result!!.residentId shouldBe "jose"
                result.baselineReady shouldBe false
            }

            then("solo tiene finding de baseline formando") {
                result!!.findings shouldHaveSize 1
                result.findings[0].code shouldBe "BASELINE_FORMING"
            }
        }
    }

    // ── Historia 3: datos de sueño generan findings ─────────────────────────
    given("una residente con 14 días de datos de sueño pobres") {
        val sleepDays = (1..14).map { n ->
            aSleepSummary(joseId, LocalDate.of(2026, 8, 1).plusDays(n.toLong()),
                calm = 200, restless = 120, awake = 60, exits = 4, wakes = 5)
        }

        val service = FindingService(
            residentRepository = mockk<ResidentRepository> {
                every { findById(joseId) } returns jose
            },
            summaryRepository = mockk<SummaryRepository> {
                every { findSleepByResidentAndRange(any(), any(), any()) } returns sleepDays
                every { findBathroomByResidentAndRange(any(), any(), any()) } returns emptyList()
            },
            sceneEventRepository = mockk<SceneEventRepository> {
                every { findByResidentId(any(), any<Instant>(), any<Instant>()) } returns emptyList()
            },
            careSummaryRepository = mockk<CareSummaryRepository> {
                every { findByResidentAndRange(any(), any(), any()) } returns emptyList()
            },
            historyEpisodeRepository = mockk<HistoryEpisodeDetectionRepository> {
                every { findByResidentId(any()) } returns emptyList()
            },
            alarmProfileRepository = mockk<AlarmProfileRepository> {
                every { findCurrentByResidentId(any()) } returns aProfile(joseId, RiskLevel.MEDIUM)
            },
            alarmOverrideRepository = mockk<AlarmProfileOverrideRepository> {
                every { findByProfileVersionId(any()) } returns emptyList()
            },
            findingPolicyService = mockk(relaxed = true),
            properties = properties,
        )

        `when`("se pide el briefing de 30 días") {
            val from = LocalDate.of(2026, 8, 1)
            val to = LocalDate.of(2026, 8, 30)
            val result = service.residentBriefing("jose", from, to)

            then("el briefing tiene findings") {
                result.shouldNotBeNull()
                result!!.findings.shouldNotBeNull()
                result.findings.isNotEmpty() shouldBe true
            }

            then("el briefing tiene KPI cards de sueño") {
                result!!.sleepCards.shouldNotBeNull()
                result.sleepCards.isNotEmpty() shouldBe true
            }

            then("el briefing tiene narrativa") {
                result!!.narrative.shouldNotBeNull()
            }
        }
    }

    // ── Historia 4: alto riesgo con override genera policy lines ────────────
    given("una residente de alto riesgo con DwellOverride de 30/45 minutos") {
        val overrides = listOf(
            PolicyOverride.DwellOverride(
                id = Identifier.random(), ruleId = "dwell-override-1",
                observeOnly = false, severity = "HIGH", closureCondition = null,
                stateKind = "out_of_bed", warningAfterMinutes = 30, alertAfterMinutes = 45,
            )
        )

        val service = FindingService(
            residentRepository = mockk<ResidentRepository> {
                every { findById(mariaId) } returns maria
            },
            summaryRepository = mockk<SummaryRepository> {
                every { findSleepByResidentAndRange(any(), any(), any()) } returns emptyList()
                every { findBathroomByResidentAndRange(any(), any(), any()) } returns emptyList()
            },
            sceneEventRepository = mockk<SceneEventRepository> {
                every { findByResidentId(any(), any<Instant>(), any<Instant>()) } returns emptyList()
            },
            careSummaryRepository = mockk<CareSummaryRepository> {
                every { findByResidentAndRange(any(), any(), any()) } returns emptyList()
            },
            historyEpisodeRepository = mockk<HistoryEpisodeDetectionRepository> {
                every { findByResidentId(any()) } returns emptyList()
            },
            alarmProfileRepository = mockk<AlarmProfileRepository> {
                every { findCurrentByResidentId(mariaId) } returns aProfile(mariaId, RiskLevel.HIGH)
            },
            alarmOverrideRepository = mockk<AlarmProfileOverrideRepository> {
                every { findByProfileVersionId(any()) } returns overrides
            },
            findingPolicyService = mockk(relaxed = true),
            properties = properties,
        )

        `when`("se pide el briefing") {
            val from = LocalDate.of(2026, 8, 1)
            val to = LocalDate.of(2026, 8, 30)
            val result = service.residentBriefing("maria", from, to)

            then("el briefing incluye policyToday con reglas de alto riesgo") {
                result.shouldNotBeNull()
                result!!.policyToday.shouldNotBeNull()
                result.policyToday.isNotEmpty() shouldBe true
            }
        }
    }

    // ── Historia 5: episodios cercanos a salidas de cama ────────────────────
    given("una residente con episodio de caída 10 minutos después de una salida de cama") {
        val exitTime = Instant.parse("2026-08-25T05:15:00Z")
        val episodeTime = exitTime.plusSeconds(600) // 10 minutos después

        val sceneEvents = listOf(
            aSceneEvent(joseId, exitTime.minusSeconds(300), SceneState.SLEEPING, SceneState.EMPTY),
            aSceneEvent(joseId, exitTime, SceneState.EMPTY, SceneState.SLEEPING),
        )
        val episodes = listOf(
            anEpisode(joseId, episodeTime, EpisodeKind.FALL, HistoryEpisodeSeverity.CRITICAL)
        )

        val service = FindingService(
            residentRepository = mockk<ResidentRepository> {
                every { findById(joseId) } returns jose
            },
            summaryRepository = mockk<SummaryRepository> {
                every { findSleepByResidentAndRange(any(), any(), any()) } returns emptyList()
                every { findBathroomByResidentAndRange(any(), any(), any()) } returns emptyList()
            },
            sceneEventRepository = mockk<SceneEventRepository> {
                every { findByResidentId(any(), any<Instant>(), any<Instant>()) } returns sceneEvents
            },
            careSummaryRepository = mockk<CareSummaryRepository> {
                every { findByResidentAndRange(any(), any(), any()) } returns emptyList()
            },
            historyEpisodeRepository = mockk<HistoryEpisodeDetectionRepository> {
                every { findByResidentId(joseId) } returns episodes
            },
            alarmProfileRepository = mockk<AlarmProfileRepository> {
                every { findCurrentByResidentId(any()) } returns null
            },
            alarmOverrideRepository = mockk<AlarmProfileOverrideRepository>(),
            findingPolicyService = mockk(relaxed = true),
            properties = properties,
        )

        `when`("se pide el reporte") {
            val from = LocalDate.of(2026, 8, 20)
            val to = LocalDate.of(2026, 8, 30)
            val result = service.residentReport("jose", from, to)

            then("el reporte incluye el episodio referenciado") {
                result.shouldNotBeNull()
                result!!.episodes shouldHaveSize 1
                result.episodes[0].kind shouldBe "FALL"
            }
        }
    }

    // ── Historia 6: facility briefing agrega múltiples residentes ───────────
    given("una instalación con 2 residentes activas recién admitidas") {
        val joseRecent = Resident.reconstitute(
            id = joseId, externalId = "ext-jose", fullName = "José García",
            birthDate = LocalDate.of(1940, 3, 15),
            admissionDate = LocalDate.now().minusDays(3),
            status = com.hub.population.domain.model.ResidentStatus.ACTIVE,
            dischargedAt = null, dischargedBy = null, version = 0,
        )
        val mariaRecent = Resident.reconstitute(
            id = mariaId, externalId = "ext-maria", fullName = "María López",
            birthDate = LocalDate.of(1938, 7, 22),
            admissionDate = LocalDate.now().minusDays(5),
            status = com.hub.population.domain.model.ResidentStatus.ACTIVE,
            dischargedAt = null, dischargedBy = null, version = 0,
        )
        val service = FindingService(
            residentRepository = mockk<ResidentRepository> {
                every { findAll() } returns listOf(joseRecent, mariaRecent)
                every { findById(joseId) } returns joseRecent
                every { findById(mariaId) } returns mariaRecent
            },
            summaryRepository = mockk<SummaryRepository> {
                every { findSleepByResidentAndRange(any(), any(), any()) } returns emptyList()
                every { findBathroomByResidentAndRange(any(), any(), any()) } returns emptyList()
            },
            sceneEventRepository = mockk<SceneEventRepository> {
                every { findByResidentId(any(), any<Instant>(), any<Instant>()) } returns emptyList()
            },
            careSummaryRepository = mockk<CareSummaryRepository> {
                every { findByResidentAndRange(any(), any(), any()) } returns emptyList()
            },
            historyEpisodeRepository = mockk<HistoryEpisodeDetectionRepository> {
                every { findByResidentId(any()) } returns emptyList()
            },
            alarmProfileRepository = mockk<AlarmProfileRepository> {
                every { findCurrentByResidentId(any()) } returns null
            },
            alarmOverrideRepository = mockk<AlarmProfileOverrideRepository>(),
            findingPolicyService = mockk(relaxed = true),
            properties = properties,
        )

        `when`("se pide el briefing de la instalación") {
            val from = LocalDate.of(2026, 8, 1)
            val to = LocalDate.of(2026, 8, 30)
            val result = service.facilityBriefing(from, to)

            then("cuenta las 2 residentes") {
                result.residentCount shouldBe 2
            }

            then("ambas están en baseline forming") {
                result.baselineForming shouldBe 2
            }
        }
    }

    // ── Historia 7: resolveWindow ───────────────────────────────────────────
    given("se pide resolver ventana sin fechas explícitas") {
        val service = FindingService(
            residentRepository = mockk(relaxed = true),
            summaryRepository = mockk(relaxed = true),
            sceneEventRepository = mockk(relaxed = true),
            careSummaryRepository = mockk(relaxed = true),
            historyEpisodeRepository = mockk(relaxed = true),
            alarmProfileRepository = mockk(relaxed = true),
            alarmOverrideRepository = mockk(relaxed = true),
            findingPolicyService = mockk(relaxed = true),
            properties = properties,
        )

        `when`("days=14 y no hay from/to") {
            val (start, end) = service.resolveWindow(null, null, 14)

            then("el rango es de 14 días hasta hoy") {
                end shouldBe LocalDate.now(properties.zoneId)
                start shouldBe end.minusDays(13)
            }
        }

        `when`("days=1 y no hay from/to") {
            val (start, end) = service.resolveWindow(null, null, 1)

            then("start y end son el mismo día") {
                start shouldBe end
            }
        }

        `when`("from y to son explícitos") {
            val from = LocalDate.of(2026, 8, 1)
            val to = LocalDate.of(2026, 8, 15)
            val (start, end) = service.resolveWindow(from, to, 14)

            then("se respeta el rango explícito") {
                start shouldBe from
                end shouldBe to
            }
        }
    }

    // ── Historia 8: residente descargada no aparece en facility ─────────────
    given("una instalación con una residente activa y una descargada") {
        val discharged = Resident.reconstitute(
            id = ResidentId.from("ana"), externalId = "ext-ana", fullName = "Ana Ruiz",
            birthDate = LocalDate.of(1942, 5, 10), admissionDate = LocalDate.of(2025, 1, 1),
            status = com.hub.population.domain.model.ResidentStatus.DISCHARGED,
            dischargedAt = Instant.now(), dischargedBy = "admin", version = 1,
        )

        val service = FindingService(
            residentRepository = mockk<ResidentRepository> {
                every { findAll() } returns listOf(jose, discharged)
                every { findById(joseId) } returns jose
            },
            summaryRepository = mockk<SummaryRepository> {
                every { findSleepByResidentAndRange(any(), any(), any()) } returns emptyList()
                every { findBathroomByResidentAndRange(any(), any(), any()) } returns emptyList()
            },
            sceneEventRepository = mockk<SceneEventRepository> {
                every { findByResidentId(any(), any<Instant>(), any<Instant>()) } returns emptyList()
            },
            careSummaryRepository = mockk<CareSummaryRepository> {
                every { findByResidentAndRange(any(), any(), any()) } returns emptyList()
            },
            historyEpisodeRepository = mockk<HistoryEpisodeDetectionRepository> {
                every { findByResidentId(any()) } returns emptyList()
            },
            alarmProfileRepository = mockk<AlarmProfileRepository> {
                every { findCurrentByResidentId(any()) } returns null
            },
            alarmOverrideRepository = mockk<AlarmProfileOverrideRepository>(),
            findingPolicyService = mockk(relaxed = true),
            properties = properties,
        )

        `when`("se pide el briefing de la instalación") {
            val from = LocalDate.of(2026, 8, 1)
            val to = LocalDate.of(2026, 8, 30)
            val result = service.facilityBriefing(from, to)

            then("solo cuenta la residente activa") {
                result.residentCount shouldBe 1
            }
        }
    }
})
