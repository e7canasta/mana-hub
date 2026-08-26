package com.hub.blueprints.scenarios

import com.hub.blueprints.supporting.*
import com.hub.clients.core.manahub
import com.hub.clients.observation.PerceptionKind
import java.time.LocalDate

/**
 * BLUEPRINT: Perception Ingestion
 * Context Group: Clinical Monitoring
 *
 * Chain: PERCEPTION → (passes through scene engine)
 *
 * Flow: camera detects resident postures → perceptions are registered
 *
 * Validates:
 *   Observation: registerPerception() with typed kinds
 *   Observation: wingBoard() for current state
 */
object PercepcionIngestion {
    @JvmStatic
    fun main() {
        val hub = manahub("http://localhost:8080") {}

        banner("Perception Ingestion", "Clinical Monitoring")

        // ── 1. Setup ──
        step(1, "Setup Residence and Resident")
        val facility = hub.residence.setupFacility(TestData.facilityName("Residencia Sensores")) {
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

        // ── 2. Cameras emit perceptions ──
        separator()
        step(2, "Perceptions: Cameras Detect Postures")

        hub.observation.registerPerception(
            monitorKey = "cam-101",
            kind = PerceptionKind.POSTURE,
            bedId = bed.id,
            residentId = maria.id,
            state = "in_bed",
            sleeping = true
        )
        ok("Perception #1: María lying down sleeping")

        hub.observation.registerPerception(
            monitorKey = "cam-101",
            kind = PerceptionKind.POSTURE,
            bedId = bed.id,
            residentId = maria.id,
            state = "sitting",
            sleeping = false
        )
        ok("Perception #2: María sitting on bed edge")

        hub.observation.registerPerception(
            monitorKey = "cam-101",
            kind = PerceptionKind.LOCATION,
            bedId = bed.id,
            residentId = maria.id,
            state = "standing",
            sleeping = false
        )
        ok("Perception #3: María standing next to bed")

        // ── 3. Verify perceptions on wing board ──
        separator()
        step(3, "Verify Wing Board")
        val wing = facility.tree().wings.first()
        val board = hub.observation.wingBoard(wing.wing.id)
        data("  Beds on board", board.size)
        board.forEach { state ->
            data("  Bed ${state.bedId}", "state=${state.state}, sleeping=${state.sleeping}")
        }

        // ── Summary ──
        summary("PERCEPTION INGESTION COMPLETE", mapOf(
            "Resident" to maria.fullName,
            "Perceptions registered" to 3,
            "Types" to "POSTURE, LOCATION",
            "States" to "in_bed → sitting → standing"
        ))
    }
}
