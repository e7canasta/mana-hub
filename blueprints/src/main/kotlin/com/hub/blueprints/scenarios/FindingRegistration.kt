package com.hub.blueprints.scenarios

import com.hub.blueprints.supporting.*
import com.hub.clients.care.ResidentNoteType
import com.hub.clients.core.manahub
import java.time.LocalDate

/**
 * BLUEPRINT: Finding Registration
 * Context Group: Clinical History
 *
 * Chain: SCENE CHANGE → EPISODE → FINDING (clinical insight)
 *
 * Flow: nurse observes pattern → registers findings → queries history
 *
 * Validates:
 *   Care: registerFinding(), findings()
 *   Care: addResidentNote() with typed ResidentNoteType
 */
object FindingRegistration {
    @JvmStatic
    fun main() {
        val hub = manahub("http://localhost:8080") {}

        banner("Finding Registration", "Clinical History")

        // ── 1. Setup ──
        step(1, "Setup Residence and Resident")
        val facility = hub.residence.setupFacility(TestData.facilityName("Residencia Hallazgos")) {
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

        // ── 2. Register clinical finding (INSIGHT) ──
        separator()
        step(2, "Register Clinical Finding: INSIGHT")
        hub.care.registerFinding(
            residentId = maria.id,
            authorId = "nurse_clinica",
            findingType = ResidentNoteType.INSIGHT,
            body = "Resident shows improved mobility after 2 weeks of physical therapy"
        )
        ok("Finding registered: INSIGHT")

        // ── 3. Register pattern finding ──
        separator()
        step(3, "Register Finding: PATTERN")
        hub.care.registerFinding(
            residentId = maria.id,
            authorId = "nurse_clinica",
            findingType = ResidentNoteType.PATTERN,
            body = "Increased night-time bathroom visits over the past 3 days (2 → 3 → 4)"
        )
        ok("Finding registered: PATTERN")

        // ── 4. Register observation finding ──
        separator()
        step(4, "Register Finding: OBSERVATION")
        hub.care.registerFinding(
            residentId = maria.id,
            authorId = "nurse_clinica",
            findingType = ResidentNoteType.OBSERVATION,
            body = "Resident refused breakfast today, possible loss of appetite"
        )
        ok("Finding registered: OBSERVATION")

        // ── 5. Register recommendation ──
        separator()
        step(5, "Register Finding: RECOMMENDATION")
        hub.care.registerFinding(
            residentId = maria.id,
            authorId = "nurse_clinica",
            findingType = ResidentNoteType.INSIGHT,
            body = "Schedule nutritionist consultation due to decreased appetite"
        )
        ok("Finding registered: RECOMMENDATION")

        // ── 6. Query findings ──
        separator()
        step(6, "Query Resident Findings")
        val findings = hub.care.findings(maria.id)
        data("  Total findings", findings.size)
        findings.forEach { note ->
            data("  [${note.kind}]", note.body)
        }

        // ── 7. Add general resident note ──
        separator()
        step(7, "Add Resident Note: CARE")
        hub.care.addResidentNote(
            residentId = maria.id,
            authorId = "caregiver_ana",
            kind = ResidentNoteType.CARE,
            body = "Resident ate light lunch, seemed in good spirits"
        )
        ok("Resident note registered")

        // ── Summary ──
        summary("FINDING REGISTRATION COMPLETE", mapOf(
            "Resident" to maria.fullName,
            "Findings registered" to 4,
            "Types" to "INSIGHT, PATTERN, OBSERVATION, RECOMMENDATION",
            "Notes total" to findings.size.plus(1).toString()
        ))
    }
}
