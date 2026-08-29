package com.hub.blueprints.scenarios

import com.hub.blueprints.supporting.*
import com.hub.clients.core.manahub
import com.hub.clients.observation.PerceptionKind
import com.hub.clients.observation.SceneChangeKind
import com.hub.clients.observation.TriggerType
import java.time.LocalDate

/**
 * BLUEPRINT: Scene Change Flow
 * Context Group: Clinical Monitoring
 *
 * Chain: PERCEPCIÓN → CAMBIO DE ESCENA → (puede generar EPISODIO)
 *
 * Flow:
 *   1. Cámara detecta percepción (sensor crudo)
 *   2. Motor de escena aplica hysteresis y confirma
 *   3. Cambio de escena registrado (transición confirmada)
 *   4. Historia de cambios consultada
 *
 * Implemented 2026-08-29: POST /internal/v1/scene-events + GET /api/v1/residents/{id}/scene-events
 *
 * Validates:
 *   Observation: registerPerception()
 *   Observation: registerSceneChange()
 *   Observation: sceneChanges()
 */
object CambioDeEscenaFlow {
    @JvmStatic
    fun main() {
        val hub = manahub("http://localhost:8080") {}

        banner("Scene Change Flow", "Clinical Monitoring")

        // ── 1. Setup ──
        step(1, "Setup Residence and Resident")
        val facility = hub.residence.setupFacility(TestData.facilityName("Residencia Escena")) {
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

        // ── 2. Camera detects raw perception ──
        separator()
        step(2, "Perception: Camera Detects Movement")
        hub.observation.registerPerception(
            monitorKey = "cam-101",
            kind = PerceptionKind.LOCATION,
            bedId = bed.id,
            residentId = maria.id,
            state = "out_of_bed",
            sleeping = false
        )
        ok("Perception registered: out_of_bed")

        // ── 3. Scene engine confirms (hysteresis) ──
        separator()
        step(3, "Scene Change: Engine Confirms Transition")
        info("Scene engine applies hysteresis (threshold: 5s)")
        info("Transition confirmed: in_bed → out_of_bed")

        hub.observation.registerSceneChange(
            bedId = bed.id,
            residentId = maria.id,
            kind = SceneChangeKind.TRANSITION,
            fromState = "in_bed",
            toState = "out_of_bed",
            triggerType = TriggerType.HYSTERESIS
        )
        ok("Scene change registered")

        // ── 4. Second change: prolonged out-of-bed ──
        separator()
        step(4, "Scene Change: Prolonged Permanence")
        info("Resident has been out of bed for 10 minutes")
        info("Scene engine detects permanence (threshold: 10min)")

        hub.observation.registerSceneChange(
            bedId = bed.id,
            residentId = maria.id,
            kind = SceneChangeKind.PERMANENCE,
            fromState = "out_of_bed",
            toState = "out_of_bed",
            triggerType = TriggerType.PERMANENCE
        )
        ok("Scene change (permanence) registered")

        // ── 5. Query scene-change history ──
        separator()
        step(5, "Query Scene Change History")
        val changes = hub.observation.sceneChanges(maria.id)
        data("  Changes registered", changes.size)
        ok("Scene changes persisted: ${changes.size} events")

        // ── Summary ──
        summary("SCENE CHANGE FLOW COMPLETE", mapOf(
            "Resident" to maria.fullName,
            "Scene changes" to 2,
            "Types" to "TRANSITION + PERMANENCE",
            "States" to "in_bed → out_of_bed (permanent)"
        ))
    }
}
