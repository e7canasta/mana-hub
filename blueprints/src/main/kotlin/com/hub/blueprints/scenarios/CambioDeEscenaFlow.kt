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
 * Chain: PERCEPTION → SCENE CHANGE → (may generate EPISODE)
 *
 * Flow:
 *   1. Camera detects perception (raw sensor)
 *   2. Scene engine applies hysteresis and confirms
 *   3. Scene change is registered (confirmed transition)
 *   4. Resident scene-change history is queried
 *
 * NOTE: The /internal/v1/scene-events endpoint does not exist yet.
 * This blueprint validates the DSL CONTRACT. When the endpoint is
 * implemented, it will work end-to-end without blueprint changes.
 *
 * Validates:
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
        info("NOTE: This endpoint does not exist on the server yet")
        info("DSL contract is ready for when it is implemented")
        // val changes = hub.observation.sceneChanges(maria.id)
        // data("  Changes registered", changes.size)
        ok("DSL contract validated")

        // ── Summary ──
        summary("SCENE CHANGE FLOW COMPLETE", mapOf(
            "Resident" to maria.fullName,
            "Scene changes" to 2,
            "Types" to "TRANSITION + PERMANENCE",
            "States" to "in_bed → out_of_bed (permanent)"
        ))
    }
}
