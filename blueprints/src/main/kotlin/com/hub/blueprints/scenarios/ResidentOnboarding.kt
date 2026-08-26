package com.hub.blueprints.scenarios

import com.hub.blueprints.supporting.*
import com.hub.blueprints.supporting.TestData
import com.hub.clients.core.manahub
import java.time.LocalDate

/**
 * BLUEPRINT: Resident Onboarding
 * Context Group: Resident Lifecycle
 *
 * Flujo: registrar usuario → crear residencia → admitting resident → asignar a cama
 *
 * Este escenario valida los contextos:
 *   Identity → Residence → Population
 */
object ResidentOnboarding {
    @JvmStatic
    fun main() {
    val hub = manahub("http://localhost:8080") {}

    banner("Resident Onboarding", "Resident Lifecycle")

    // ── 1. Registrar director médico ──
    step(1, "Registrar Director Médico")
    val director = hub.identity.registerOwner(
        username = TestData.username("director"),
        displayName = "Dr. Carlos Méndez",
        password = "admin123"
    )
    ok("Director: ${director.username} (${director.role})")

    // ── 2. Crear jerarquía de la residencia ──
    separator()
    step(2, "Crear Residencia")
    val residencia = hub.residence.setupFacility(TestData.facilityName("Residencia Esperanza")) {
        timezone = "America/Mexico_City"
        wing("Piso 1") {
            floor = "1"
            sortOrder = 1
            room("101") {
                roomType = "STANDARD"
                bed("A")
                bed("B")
            }
            room("102") {
                roomType = "STANDARD"
                bed("A")
            }
        }
        wing("Piso 2") {
            floor = "2"
            sortOrder = 2
            room("201") {
                roomType = "STANDARD"
                bed("A")
            }
        }
    }
    ok("Residencia: ${residencia.name} (${residencia.id})")

    val arbol = residencia.tree()
    ok("Alas: ${arbol.wings.size}")
    arbol.wings.forEach { alaDef ->
        data("  └─ ${alaDef.wing.name}", "${alaDef.rooms.size} habitaciones")
        alaDef.rooms.forEach { habDef ->
            data("     └─ Hab ${habDef.room.number}", "${habDef.beds.size} camas")
        }
    }

    // ── 3. Admitir residente ──
    separator()
    step(3, "Admitir Residente")
    val maria = hub.population.admitResident(
        fullName = TestData.residentName("María", "García López"),
        birthDate = LocalDate.of(1935, 3, 15),
        admissionDate = LocalDate.now()
    )
    ok("Residente: ${maria.fullName} (${maria.id})")
    data("  Status", maria.status)
    data("  Nacimiento", maria.birthDate)
    data("  Ingreso", maria.admissionDate)

    // ── 4. Asignar a cama ──
    separator()
    step(4, "Asignar a Cama")
    val cama = residencia.firstBed()
    if (cama != null) {
        val asignacion = maria.assignTo(cama.id)
        ok("Asignada a: ${cama.id}")
        data("  Asignación abierta", asignacion.isOpen)

        val asignaciones = maria.assignments()
        data("  Total asignaciones", asignaciones.size)
    } else {
        error("No hay camas disponibles")
    }

    // ── Resumen ──
    summary("RESIDENT ONBOARDING COMPLETO", mapOf(
        "Residente" to maria.fullName,
        "Residencia" to residencia.name,
        "Cama" to (cama?.id ?: "Sin asignar"),
        "Status" to maria.status
    ))
    }
}
