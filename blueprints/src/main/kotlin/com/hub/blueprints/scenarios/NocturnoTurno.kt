package com.hub.blueprints.scenarios

import com.hub.blueprints.supporting.*
import com.hub.clients.core.manahub
import com.hub.clients.observation.BathroomSummaryData
import com.hub.clients.observation.MobilitySummaryData
import com.hub.clients.observation.PerceptionKind
import com.hub.clients.observation.SleepSummaryData
import com.hub.clients.surveillance.EpisodeSeverity
import com.hub.clients.surveillance.EpisodeStatus
import java.time.Instant
import java.time.LocalDate

/**
 * BLUEPRINT: Night Shift
 * Context Group: Clinical Monitoring + Care
 *
 * Flow: night shift → perception monitoring → episode → clinical summaries
 *
 * Validates:
 *   Residence → Population → Observation → Surveillance → Care
 */
object NocturnoTurno {
    @JvmStatic
    fun main() {
        val hub = manahub("http://localhost:8080") {}

        banner("Night Shift", "Clinical Monitoring + Care")

        // ── 1. Setup ──
        step(1, "Setup Residence and Residents")
        val facility = hub.residence.setupFacility(TestData.facilityName("Residencia Nocturna")) {
            timezone = "America/Mexico_City"
            wing("Piso 1") {
                floor = "1"
                room("101") { bed("A") }
                room("102") { bed("A") }
            }
        }
        val maria = hub.population.admitResident(
            fullName = TestData.residentName("María", "García"),
            birthDate = LocalDate.of(1935, 3, 15),
            admissionDate = LocalDate.now()
        )
        val juan = hub.population.admitResident(
            fullName = TestData.residentName("Juan", "Pérez"),
            birthDate = LocalDate.of(1940, 5, 20),
            admissionDate = LocalDate.now()
        )
        ok("Residents: ${maria.fullName}, ${juan.fullName}")

        // ── 2. Night shift starts ──
        separator()
        step(2, "Night Shift Starts")
        info("Start time: ${Instant.now()}")

        // ── 3. Query wing board ──
        separator()
        step(3, "Wing Board")
        val tree = facility.tree()
        val wing = tree.wings.first()
        val board = hub.observation.wingBoard(wing.wing.id)
        data("  Beds on wing", board.size)

        // ── 4. Register perceptions (simulated by camera) ──
        separator()
        step(4, "Camera Perceptions")
        hub.observation.registerPerception(
            monitorKey = "cam-101",
            kind = PerceptionKind.POSTURE,
            bedId = "test-bed-1",
            residentId = maria.id,
            state = "in_bed",
            sleeping = true
        )
        ok("Perception: ${maria.fullName} sleeping in bed")

        hub.observation.registerPerception(
            monitorKey = "cam-102",
            kind = PerceptionKind.POSTURE,
            bedId = "test-bed-2",
            residentId = juan.id,
            state = "in_bed",
            sleeping = true
        )
        ok("Perception: ${juan.fullName} sleeping in bed")

        // ── 5. Episode detected ──
        separator()
        step(5, "Episode: Out of Bed")
        val episode = hub.surveillance.registerEpisode(maria.id) {
            severity = EpisodeSeverity.WARNING
            title = "Out of bed detected"
            detail = "Resident got up at 02:15"
        }
        ok("Episode: ${episode.title} (${episode.severity})")

        // ── 6. Nurse responds ──
        separator()
        step(6, "Nurse Response")
        episode.acknowledge("nurse_nocturno")
        ok("Episode acknowledged")
        episode.resolve(EpisodeStatus.RESOLVED)
        ok("Episode resolved: resident back in bed")

        // ── 7. Clinical summaries ──
        separator()
        step(7, "Clinical Summaries")
        hub.observation.ingestSleepSummary(maria.id, TestData.yesterday(), SleepSummaryData(
            calmMinutes = 360,
            restlessMinutes = 45,
            awakeMinutes = 30,
            outOfBedMinutes = 15,
            bedExitCount = 2,
            wakeCount = 3
        ))
        ok("Sleep summary registered")

        hub.observation.ingestMobilitySummary(maria.id, TestData.yesterday(), MobilitySummaryData(
            inBedMinutes = 400,
            outOfBedMinutes = 40,
            outOfSightMinutes = 10,
            walkingMinutes = 20,
            distanceMeters = 150.0,
            transferCount = 3
        ))
        ok("Mobility summary registered")

        hub.observation.ingestBathroomSummary(maria.id, TestData.yesterday(), BathroomSummaryData(
            visitCount = 3,
            nightVisitCount = 2,
            assistedCount = 0,
            totalMinutes = 25
        ))
        ok("Bathroom summary registered")

        // ── 8. Query summaries ──
        separator()
        step(8, "Query Summaries")
        val sleep = hub.observation.sleepSummary(maria.id)
        val mobility = hub.observation.mobilitySummary(maria.id)
        val bathroom = hub.observation.bathroomSummary(maria.id)

        data("  Sleep", "${sleep?.calmMinutes} calm, ${sleep?.restlessMinutes} restless")
        data("  Mobility", "${mobility?.walkingMinutes} walking, ${mobility?.distanceMeters}m")
        data("  Bathroom", "${bathroom?.visitCount} visits, ${bathroom?.nightVisitCount} night")

        // ── Summary ──
        summary("NIGHT SHIFT COMPLETE", mapOf(
            "Residents monitored" to 2,
            "Episodes" to "1 (resolved)",
            "Clinical summaries" to "3 registered"
        ))
    }
}
