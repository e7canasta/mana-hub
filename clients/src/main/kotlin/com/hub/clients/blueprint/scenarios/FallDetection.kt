package com.hub.clients.blueprint.scenarios

import com.hub.clients.care.EpisodeNoteKind
import com.hub.clients.core.manahub
import com.hub.clients.surveillance.EpisodeSeverity
import com.hub.clients.surveillance.EpisodeStatus
import java.time.LocalDate

fun main() {
    val hub = manahub("http://localhost:8080") {}

    println("═══════════════════════════════════════════")
    println("  BLUEPRINT: Fall Detection")
    println("  Context Group: Clinical Monitoring")
    println("═══════════════════════════════════════════\n")

    // 1. Setup: Create facility, admit resident, configure policy
    println("═══ Step 1: Setup ═══")
    val facility = hub.residence.setupFacility("Residencia Segura") {
        timezone = "America/Mexico_City"
        wing("Piso 1") {
            floor = "1"
            room("101") {
                bed("A")
            }
        }
    }
    println("  ✓ Facility: ${facility.name}")

    val resident = hub.population.admitResident(
        fullName = "María García López",
        birthDate = LocalDate.of(1935, 3, 15),
        admissionDate = LocalDate.now()
    )
    println("  ✓ Resident: ${resident.fullName}")

    val bed = facility.firstBed()
    resident.assignTo(bed!!.id)
    println("  ✓ Assigned to bed: ${bed.id}")

    val profile = hub.policy.configureAlarmProfile(resident.id) {
        mobilityAid = "walker"
        autopilot = true
        mode = "fall_risk"
        templateId = "elderly_fall_risk"
        riskLevel = com.hub.clients.policy.RiskLevel.HIGH
    }
    println("  ✓ Policy configured: ${profile.riskLevel}")

    // 2. Camera detects fall (perception)
    println("\n═══ Step 2: Perception (Camera Detects Fall) ═══")
    println("  📷 Camera cam-101 detects fall event")
    println("  📊 Confidence: 92%")
    println("  🕐 Time: ${java.time.Instant.now()}")

    // 3. Episode engine evaluates and triggers episode
    println("\n═══ Step 3: Episode Engine Triggers Episode ═══")
    val episode = hub.surveillance.triggerEpisode(resident.id) {
        severity = EpisodeSeverity.CRITICAL
        title = "Caída detectada"
        detail = "DL detectó caída con 92% de confianza"
        evidenceKind = "clip"
        evidenceRef = "cam-101:${java.time.Instant.now()}"
    }
    println("  ✓ Episode triggered: ${episode.id}")
    println("  ✓ Severity: ${episode.severity}")
    println("  ✓ Status: ${episode.status}")

    // 4. Notification sent to staff
    println("\n═══ Step 4: Notification Sent ═══")
    println("  📱 SMS sent to nurse María")
    println("  🔔 Push notification to all staff on duty")

    // 5. Nurse acknowledges
    println("\n═══ Step 5: Nurse Acknowledges ═══")
    episode.acknowledge("nurse_maría")
    println("  ✓ Episode acknowledged by nurse_maría")
    println("  ✓ Status: ${episode.status}")

    // 6. Nurse adds clinical note
    println("\n═══ Step 6: Clinical Note ═══")
    episode.addNote("nurse_maría", EpisodeNoteKind.CLINICAL_NOTE, "Residente asistida, sin lesiones visibles. PA 140/90.")
    println("  ✓ Clinical note added")

    // 7. Episode resolved
    println("\n═══ Step 7: Episode Resolved ═══")
    episode.resolve(EpisodeStatus.RESOLVED)
    println("  ✓ Episode resolved")
    println("  ✓ Status: ${episode.status}")

    // 8. Summary
    println("\n═══════════════════════════════════════════")
    println("  FALL DETECTION COMPLETE")
    println("═══════════════════════════════════════════")
    println("  Resident: ${resident.fullName}")
    println("  Episode: ${episode.id}")
    println("  Severity: ${episode.severity}")
    println("  Resolution: Resolved by nurse_maría")
    println("═══════════════════════════════════════════")
}
