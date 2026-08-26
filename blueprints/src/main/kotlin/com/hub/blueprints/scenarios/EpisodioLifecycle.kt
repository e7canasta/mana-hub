package com.hub.blueprints.scenarios

import com.hub.blueprints.supporting.*
import com.hub.clients.care.EpisodeNoteKind
import com.hub.clients.core.manahub
import com.hub.clients.surveillance.EpisodeSeverity
import com.hub.clients.surveillance.EpisodeStatus
import java.time.LocalDate

/**
 * BLUEPRINT: Episode Lifecycle
 * Context Group: Clinical Monitoring
 *
 * Chain: PERCEPTION → SCENE CHANGE → EPISODE → RESOLUTION
 *
 * Flow: camera detects fall → CRITICAL episode → nurse acknowledges → clinical note → resolves
 *
 * Validates:
 *   Residence → Population → Policy → Surveillance → Care
 */
object EpisodioLifecycle {
    @JvmStatic
    fun main() {
        val hub = manahub("http://localhost:8080") {}

        banner("Episode Lifecycle", "Clinical Monitoring")

        // ── 1. Setup: facility + resident + profile ──
        step(1, "Setup Residence and Resident")
        val facility = hub.residence.setupFacility(TestData.facilityName("Residencia Segura")) {
            timezone = "America/Mexico_City"
            wing("Piso 1") {
                floor = "1"
                room("101") { bed("A") }
            }
        }
        val maria = hub.population.admitResident(
            fullName = TestData.residentName("María", "García López"),
            birthDate = LocalDate.of(1935, 3, 15),
            admissionDate = LocalDate.now()
        )
        val bed = facility.firstBed()
        maria.assignTo(bed!!.id)
        ok("Resident ${maria.fullName} assigned to bed ${bed.label}")

        // ── 2. Configure monitoring profile ──
        separator()
        step(2, "Configure Monitoring Profile")
        val profile = hub.policy.configureAlarmProfile(maria.id) {
            mobilityAid = "walker"
            autopilot = true
            mode = "fall_risk"
            templateId = "elderly_fall_risk"
            riskLevel = com.hub.clients.policy.RiskLevel.HIGH
        }
        ok("Profile configured: risk=${profile.riskLevel}, autopilot=${profile.autopilot}")

        // ── 3. Camera detects fall (raw perception) ──
        separator()
        step(3, "Perception: Camera Detects Fall")
        println("  Camera cam-101 detects fall event")
        println("  Confidence: 92%")
        println("  Time: ${java.time.Instant.now()}")

        // ── 4. Scene engine confirms (scene change) ──
        separator()
        step(4, "Scene Change: Transition Confirmed")
        println("  Scene engine applies hysteresis")
        println("  Transition confirmed: in_bed → fallen")

        // ── 5. Register the episode ──
        separator()
        step(5, "Register CRITICAL Episode")
        val episode = hub.surveillance.registerEpisode(maria.id) {
            severity = EpisodeSeverity.CRITICAL
            title = "Fall detected"
            detail = "DL detected fall with 92% confidence"
            evidenceKind = "clip"
            evidenceRef = "cam-101:${java.time.Instant.now()}"
        }
        ok("Episode registered: ${episode.id}")
        data("  Severity", episode.severity)
        data("  Status", episode.status)
        data("  Title", episode.title)

        // ── 6. Nurse acknowledges ──
        separator()
        step(6, "Nurse Acknowledges Episode")
        episode.acknowledge("nurse_maría")
        ok("Episode acknowledged by nurse_maría")
        data("  Status", episode.status)

        // ── 7. Nurse adds clinical note ──
        separator()
        step(7, "Episode Clinical Note")
        episode.addNote("nurse_maría", EpisodeNoteKind.CLINICAL_NOTE, "Resident assisted, no visible injuries. BP 140/90.")
        ok("Clinical note registered")

        // ── 8. Episode resolves ──
        separator()
        step(8, "Resolve Episode")
        episode.resolve(EpisodeStatus.RESOLVED)
        ok("Episode resolved")
        data("  Status", episode.status)

        // ── Summary ──
        summary("EPISODE LIFECYCLE COMPLETE", mapOf(
            "Resident" to maria.fullName,
            "Episode" to episode.id,
            "Severity" to episode.severity,
            "Resolution" to "Resolved by nurse_maría"
        ))
    }
}
