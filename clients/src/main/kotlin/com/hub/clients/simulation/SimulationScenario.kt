package com.hub.clients.simulation

import com.hub.clients.core.manahub
import com.hub.clients.observation.BathroomSummaryData
import com.hub.clients.observation.MobilitySummaryData
import com.hub.clients.observation.SleepSummaryData
import com.hub.clients.policy.RiskLevel
import com.hub.clients.surveillance.EpisodeSeverity
import com.hub.clients.surveillance.EpisodeStatus
import java.time.Instant
import java.time.LocalDate

/**
 * ══════════════════════════════════════════════════════════════════════════════
 *  SIMULACIÓN: Flujo Completo con Componentes Externos
 * ══════════════════════════════════════════════════════════════════════════════
 *
 *  Este escenario demuestra cómo los componentes externos interactúan con
 *  mana-hub a través de nuestro DSL.
 *
 *  Arquitectura:
 *    CÁMARA → ObservationEngine → mana-hub (persiste)
 *                   ↓
 *              EpisodeEngine → mana-hub (episodios)
 *                   ↓
 *          NotificationService → mana-hub (notificaciones)
 *                   ↓
 *            EvidenceCollector → mana-hub (evidencia)
 *
 *  NOTA: Los componentes externos son DUMMY para simulación.
 *        En producción, serían servicios reales.
 *        Nuestro DSL es el contrato que ELLOS usan para hablarnos.
 */
fun main() {

    println("╔══════════════════════════════════════════════════════════════╗")
    println("║  SIMULACIÓN: Flujo Completo con Componentes Externos       ║")
    println("╚══════════════════════════════════════════════════════════════╝")

    manahub("http://localhost:8080") {

        // ══════════════════════════════════════════════════════════
        //  SETUP: Configurar la residencia
        // ══════════════════════════════════════════════════════════
        println("\n── Setup: Configurar Residencia ──")

        val director = identity.registerOwner(
            username = "director-${System.currentTimeMillis()}",
            displayName = "Dr. Carlos Méndez"
        )
        val enfermero = identity.registerStaff(
            username = "enfermero-${System.currentTimeMillis()}",
            displayName = "Enf. Roberto García"
        )

        val residencia = residence.setupFacility("Residencia Sol Naciente ${System.currentTimeMillis()}") {
            timezone = "America/Argentina/Buenos_Aires"
            wing("Ala Norte") {
                floor = "1"
                room("101") { bed("Cama 1") }
            }
        }
        val tree = residencia.tree()
        val room = tree.wings[0].rooms[0].room
        val bed = tree.wings[0].rooms[0].beds[0]

        val camara = streams.assignStreamToRoom(room.id) {
            streamKey = "rtsp://192.168.1.101:554/stream"
            name = "Camara Room 101"
        }
        camara.defineRegions {
            bed(points = "0,0,100,0,100,150,0,150", label = "Zona cama")
            hallway(points = "100,0,200,0,200,150,100,150", label = "Pasillo")
        }

        val maria = population.admitResident {
            fullName = "María Fernández"
            birthDate = LocalDate.of(1940, 3, 15)
            admissionDate = LocalDate.now().minusDays(30)
        }
        maria.assignTo(bed.id)

        policy.configureAlarmProfile(maria.id) {
            templateId = "fall_risk"
            riskLevel = RiskLevel.HIGH
            autopilot = true
            mobilityAid = "walker"
            updatedBy = director.id
        }

        println("   Residencia: ${residencia.name}")
        println("   Room: ${room.number}, Bed: ${bed.label}")
        println("   Residente: $maria")
        println("   Perfil: HIGH (fall_risk)")

        // ══════════════════════════════════════════════════════════
        //  FASE 0: Consultar Catálogo de Presets
        // ══════════════════════════════════════════════════════════
        println("\n── Fase 0: Consultar Catálogo de Presets ──")

        val catalogo = policy.catalog()
        println("   Presets disponibles: ${catalogo.presets.size}")
        catalogo.presets.forEach { preset ->
            println("     └─ ${preset.id}: ${preset.name}")
            println("        ${preset.description}")
            println("        Umbrales: ${preset.thresholds}")
        }

        // ══════════════════════════════════════════════════════════
        //  INSTANCIAR COMPONENTES EXTERNOS (DUMMY)
        // ══════════════════════════════════════════════════════════
        println("\n── Instanciar Componentes Externos ──")

        val observationEngine: ObservationEngine = DummyObservationEngine(this)
        val episodeEngine: EpisodeEngine = DummyEpisodeEngine(this)
        val notificationService: NotificationService = DummyNotificationService(this)
        val evidenceCollector: EvidenceCollector = DummyEvidenceCollector(this)

        println("   ObservationEngine: ${observationEngine::class.simpleName}")
        println("   EpisodeEngine: ${episodeEngine::class.simpleName}")
        println("   NotificationService: ${notificationService::class.simpleName}")
        println("   EvidenceCollector: ${evidenceCollector::class.simpleName}")

        // ══════════════════════════════════════════════════════════
        //  ESCENARIO 1: María se levanta de la cama
        // ══════════════════════════════════════════════════════════
        println("\n── Escenario 1: María se levanta de la cama ──")

        // 1. La cámara detecta el movimiento
        println("   1.1 Cámara detecta bed_exit")
        observationEngine.detectBedExit(bed.id, maria.id, "bed-101")

        // 2. El motor de reglas evalúa y dispara episodio
        println("   1.2 Motor de reglas evalúa")
        episodeEngine.evaluateAndTrigger(maria.id, bed.id, "bed_exit")

        // 3. El servicio de notificaciones envía alerta al enfermero
        println("   1.3 Servicio de notificaciones envía alerta")
        notificationService.notifyStaff("episode-001", enfermero.id, "push", "María fuera de cama")

        // 4. El colector de evidencia recopila clip de video
        println("   1.4 Servicio de evidencia recopila clip")
        evidenceCollector.collectVideoClip(bed.id, maria.id, Instant.now().minusSeconds(300), Instant.now())
        evidenceCollector.collectTimeline(bed.id, maria.id)

        // 5. El enfermero reconoce y resuelve
        println("   1.5 Enfermero resuelve episodio")
        val episodios = surveillance.pendingEpisodes()
        if (episodios.isNotEmpty()) {
            val episodio = surveillance.triggerEpisode(maria.id) {
                this.bedId = bed.id
                severity = EpisodeSeverity.WARNING
                title = "María fuera de cama >5 min"
                occurredAt = Instant.now()
            }
            episodio.acknowledge(enfermero.id)
            episodio.resolve(EpisodeStatus.RESOLVED)
        }

        println("   Estado actual:")
        println("     - Episodios pendientes: ${surveillance.pendingEpisodes().size}")

        // ══════════════════════════════════════════════════════════
        //  ESCENARIO 2: Resúmenes clínicos del día
        // ══════════════════════════════════════════════════════════
        println("\n── Escenario 2: Resúmenes clínicos ──")

        observationEngine.computeSleepSummary(maria.id, LocalDate.now().minusDays(1), SleepSummaryData(
            calmMinutes = 320, restlessMinutes = 80, awakeMinutes = 40,
            outOfBedMinutes = 25, bedExitCount = 3, wakeCount = 4
        ))

        observationEngine.computeMobilitySummary(maria.id, LocalDate.now().minusDays(1), MobilitySummaryData(
            walkingMinutes = 35, distanceMeters = 95.0, transferCount = 8
        ))

        observationEngine.computeBathroomSummary(maria.id, LocalDate.now().minusDays(1), BathroomSummaryData(
            visitCount = 5, nightVisitCount = 2
        ))

        println("   Resúmenes computados por ObservationEngine")

        // ══════════════════════════════════════════════════════════
        //  RESUMEN
        // ══════════════════════════════════════════════════════════
        println("\n╔══════════════════════════════════════════════════════════════╗")
        println("║  RESUMEN DE SIMULACIÓN                                      ║")
        println("╠══════════════════════════════════════════════════════════════╣")
        println("║  ObservationEngine:   Detectó bed_exit, motion, summaries   ║")
        println("║  EpisodeEngine:       Consultó catálogo + perfil → episodio ║")
        println("║  NotificationService: Envió push al enfermero              ║")
        println("║  EvidenceCollector:   Recopiló clip y timeline             ║")
        println("║  mana-hub (SOR):      Persistió TODO                       ║")
        println("╚══════════════════════════════════════════════════════════════╝")
        println("")
        println("  Flujo: Cámara → ObservationEngine → mana-hub")
        println("         → EpisodeEngine (consulta catálogo + perfil) → mana-hub")
        println("         → NotificationService → mana-hub")
        println("         → EvidenceCollector → mana-hub")
    }

    println("\n=== SIMULACIÓN COMPLETA ===")
}
