package com.hub.blueprints.scenarios

import com.hub.blueprints.supporting.*
import com.hub.blueprints.supporting.TestData
import com.hub.clients.care.ResidentNoteType
import com.hub.clients.care.ShiftNoteKind
import com.hub.clients.core.manahub
import java.time.Instant
import java.time.LocalDate

/**
 * BLUEPRINT: Ronda de Cuidados
 * Context Group: Care Operations
 *
 * Flujo: turno nocturno → iniciar ronda → completar ronda → notas de cuidado → notas de turno
 *
 * Este escenario valida los contextos:
 *   Residence → Population → Care
 */
object RondaDeCuidados {
    @JvmStatic
    fun main() {
    val hub = manahub("http://localhost:8080") {}

    banner("Ronda de Cuidados", "Care Operations")

    // ── 1. Setup ──
    step(1, "Configurar Residencia y Residentes")
    val residencia = hub.residence.setupFacility(TestData.facilityName("Residencia Turno")) {
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
    ok("Residentes: ${maria.fullName}, ${juan.fullName}")

    // ── 2. Turno nocturno comienza ──
    separator()
    step(2, "Turno Nocturno Inicia")
    info("Hora de inicio: ${Instant.now()}")

    // ── 3. Enfermero inicia ronda ──
    separator()
    step(3, "Iniciar Ronda")
    val arbol = residencia.tree()
    val ala = arbol.wings.first()
    val ronda = hub.care.startRound(ala.wing.id)
    ok("Ronda iniciada para ala: ${ala.wing.name}")
    data("  Ronda ID", ronda.id)
    data("  Status", ronda.status)

    // ── 4. Verificar ronda actual ──
    separator()
    step(4, "Verificar Ronda Actual")
    val actual = hub.care.currentRound(ala.wing.id)
    data("  Ronda actual", actual?.id)
    data("  Status", actual?.status)

    // ── 5. Listar rondas ──
    separator()
    step(5, "Listar Rondas del Ala")
    val rondas = hub.care.rounds(ala.wing.id)
    data("  Total rondas", rondas.size)

    // ── 6. Completar ronda ──
    separator()
    step(6, "Completar Ronda")
    val completada = ronda.complete("nurse_nocturno")
    ok("Ronda completada por: ${completada.completedBy}")

    // ── 7. Registrar nota de cuidado (CARE) ──
    separator()
    step(7, "Nota de Cuidado")
    hub.care.addResidentNote(
        residentId = maria.id,
        authorId = "nurse_nocturno",
        kind = ResidentNoteType.CARE,
        body = "Residente duerme bien, sin episodios de inquietud"
    )
    ok("Nota CARE registrada para ${maria.fullName}")

    // ── 8. Registrar hallazgo (PATTERN) ──
    separator()
    step(8, "Hallazgo: Patrón Detectado")
    hub.care.addResidentNote(
        residentId = maria.id,
        authorId = "nurse_nocturno",
        kind = ResidentNoteType.PATTERN,
        body = "Patrón de sueño regular, se despierta 2-3 veces por noche"
    )
    ok("Hallazgo PATTERN registrado para ${maria.fullName}")

    // ── 9. Registrar nota de turno ──
    separator()
    step(9, "Nota de Turno")
    hub.care.addShiftNote(
        facilityId = residencia.id,
        wingId = ala.wing.id,
        shiftKey = "night",
        shiftDate = LocalDate.now().toString(),
        authorId = "nurse_nocturno",
        kind = ShiftNoteKind.SHIFT_SUMMARY,
        body = "Turno nocturno sin incidentes. Residentes estables."
    )
    ok("Nota de turno registrada")

    // ── 10. Consultar notas de turno ──
    separator()
    step(10, "Consultar Notas de Turno")
    val notasTurno = hub.care.shiftNotes(residencia.id, LocalDate.now().toString())
    data("  Notas de turno", notasTurno.size)

    // ── Resumen ──
    summary("RONDA DE CUIDADOS COMPLETA", mapOf(
        "Rondas" to rondas.size,
        "Notas de cuidado" to 1,
        "Hallazgos" to 1,
        "Notas de turno" to notasTurno.size
    ))
    }
}
