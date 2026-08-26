package com.hub.clients.blueprint.scenarios

import com.hub.clients.core.manahub
import com.hub.clients.observation.BathroomSummaryData
import com.hub.clients.observation.MobilitySummaryData
import com.hub.clients.observation.PerceptionKind
import com.hub.clients.observation.SleepSummaryData
import com.hub.clients.surveillance.EpisodeSeverity
import java.time.Instant
import java.time.LocalDate

fun main() {
    val hub = manahub("http://localhost:8080") {}

    println("═══════════════════════════════════════════")
    println("  BLUEPRINT: Night Shift")
    println("  Context Group: Clinical Monitoring + Care")
    println("═══════════════════════════════════════════\n")

    // 1. Setup
    println("═══ Step 1: Setup ═══")
    val facility = hub.residence.setupFacility("Residencia Nocturna") {
        timezone = "America/Mexico_City"
        wing("Piso 1") {
            floor = "1"
            room("101") {
                bed("A")
            }
            room("102") {
                bed("A")
            }
        }
    }
    println("  ✓ Facility: ${facility.name}")

    val resident1 = hub.population.admitResident(
        fullName = "María García",
        birthDate = LocalDate.of(1935, 3, 15),
        admissionDate = LocalDate.now()
    )
    val resident2 = hub.population.admitResident(
        fullName = "Juan Pérez",
        birthDate = LocalDate.of(1940, 5, 20),
        admissionDate = LocalDate.now()
    )
    println("  ✓ Residents admitted: ${resident1.fullName}, ${resident2.fullName}")

    // 2. Night shift begins
    println("\n═══ Step 2: Night Shift Begins ═══")
    println("  🌙 Night shift started at ${Instant.now()}")

    // 3. Check wing board
    println("\n═══ Step 3: Wing Board ═══")
    val tree = facility.tree()
    val wing = tree.wings.first()
    println("  📊 Wing: ${wing.wing.name}")

    // 4. Monitor perception events
    println("\n═══ Step 4: Perception Events ═══")
    hub.observation.ingestEvent(
        monitorKey = "cam-101",
        kind = PerceptionKind.POSTURE,
        bedId = "facility-1:wing-1:room-101:bed-A",
        residentId = resident1.id,
        state = "in_bed",
        sleeping = true
    )
    println("  📷 Resident 1: sleeping in bed")

    hub.observation.ingestEvent(
        monitorKey = "cam-102",
        kind = PerceptionKind.POSTURE,
        bedId = "facility-1:wing-1:room-102:bed-A",
        residentId = resident2.id,
        state = "in_bed",
        sleeping = true
    )
    println("  📷 Resident 2: sleeping in bed")

    // 5. Episode detected
    println("\n═══ Step 5: Episode Detected ═══")
    val episode = hub.surveillance.triggerEpisode(resident1.id) {
        severity = EpisodeSeverity.WARNING
        title = "Fuera de cama detectado"
        detail = "Residente se levantó a las 02:15"
    }
    println("  ⚠️ Episode: ${episode.title} (${episode.severity})")

    // 6. Nurse acknowledges and resolves
    println("\n═══ Step 6: Nurse Response ═══")
    episode.acknowledge("nurse_nocturno")
    println("  ✓ Acknowledged by nurse_nocturno")

    episode.resolve("resident_back_to_bed")
    println("  ✓ Resolved: resident back to safe state")

    // 7. Clinical summaries
    println("\n═══ Step 7: Clinical Summaries ═══")
    hub.observation.ingestSleepSummary(resident1.id, LocalDate.now().minusDays(1), SleepSummaryData(
        calmMinutes = 360,
        restlessMinutes = 45,
        awakeMinutes = 30,
        outOfBedMinutes = 15,
        bedExitCount = 2,
        wakeCount = 3
    ))
    println("  ✓ Sleep summary ingested for ${resident1.fullName}")

    hub.observation.ingestMobilitySummary(resident1.id, LocalDate.now().minusDays(1), MobilitySummaryData(
        inBedMinutes = 400,
        outOfBedMinutes = 40,
        outOfSightMinutes = 10,
        walkingMinutes = 20,
        distanceMeters = 150.0,
        transferCount = 3
    ))
    println("  ✓ Mobility summary ingested for ${resident1.fullName}")

    hub.observation.ingestBathroomSummary(resident1.id, LocalDate.now().minusDays(1), BathroomSummaryData(
        visitCount = 3,
        nightVisitCount = 2,
        assistedCount = 0,
        totalMinutes = 25
    ))
    println("  ✓ Bathroom summary ingested for ${resident1.fullName}")

    // 8. Retrieve summaries
    println("\n═══ Step 8: Retrieve Summaries ═══")
    val sleep = hub.observation.sleepSummary(resident1.id)
    val mobility = hub.observation.mobilitySummary(resident1.id)
    val bathroom = hub.observation.bathroomSummary(resident1.id)

    println("  😴 Sleep: ${sleep?.calmMinutes} calm, ${sleep?.restlessMinutes} restless")
    println("  🚶 Mobility: ${mobility?.walkingMinutes} walking, ${mobility?.distanceMeters}m")
    println("  🚽 Bathroom: ${bathroom?.visitCount} visits, ${bathroom?.nightVisitCount} night")

    // 9. Night shift ends
    println("\n═══ Step 9: Night Shift Ends ═══")
    println("  🌅 Night shift ended at ${Instant.now()}")

    // 10. Summary
    println("\n═══════════════════════════════════════════")
    println("  NIGHT SHIFT COMPLETE")
    println("═══════════════════════════════════════════")
    println("  Residents monitored: 2")
    println("  Episodes: 1 (resolved)")
    println("  Summaries: 3 ingested")
    println("═══════════════════════════════════════════")
}
