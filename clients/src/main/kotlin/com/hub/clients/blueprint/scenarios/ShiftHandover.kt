package com.hub.clients.blueprint.scenarios

import com.hub.clients.care.ResidentNoteType
import com.hub.clients.care.ShiftNoteKind
import com.hub.clients.core.manahub
import java.time.Instant
import java.time.LocalDate

fun main() {
    val hub = manahub("http://localhost:8080") {}

    println("═══════════════════════════════════════════")
    println("  BLUEPRINT: Shift Handover")
    println("  Context Group: Clinical Care + History")
    println("═══════════════════════════════════════════\n")

    // 1. Setup
    println("═══ Step 1: Setup ═══")
    val facility = hub.residence.setupFacility("Residencia Turno") {
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

    // 3. Nurse starts round
    println("\n═══ Step 3: Nurse Starts Round ═══")
    val tree = facility.tree()
    val wing = tree.wings.first()
    val round = hub.care.startRound(wing.wing.id)
    println("  📋 Round started for wing: ${wing.wing.name}")

    // 4. Check current round
    println("\n═══ Step 4: Check Current Round ═══")
    val current = hub.care.currentRound(wing.wing.id)
    println("  📋 Current round: ${current?.id} (${current?.status})")

    // 5. List rounds
    println("\n═══ Step 5: List Rounds ═══")
    val rounds = hub.care.rounds(wing.wing.id)
    println("  📋 Total rounds: ${rounds.size}")

    // 6. Complete round
    println("\n═══ Step 6: Complete Round ═══")
    val completed = round.complete("nurse_nocturno")
    println("  ✓ Round completed by: ${completed.completedBy}")

    // 7. Add resident note (kind=CARE)
    println("\n═══ Step 7: Add Care Note ═══")
    hub.care.addResidentNote(
        residentId = resident1.id,
        authorId = "nurse_nocturno",
        kind = ResidentNoteType.CARE,
        body = "Residente duerme bien, sin episodios de inquietud"
    )
    println("  ✓ Care note added for ${resident1.fullName}")

    // 8. Add resident note
    println("\n═══ Step 8: Add Resident Note ═══")
    hub.care.addResidentNote(
        residentId = resident1.id,
        authorId = "nurse_nocturno",
        kind = ResidentNoteType.PATTERN,
        body = "Patrón de sueño regular, se despierta 2-3 veces por noche"
    )
    println("  ✓ Resident note (pattern) added for ${resident1.fullName}")

    // 9. Add shift note
    println("\n═══ Step 9: Add Shift Note ═══")
    hub.care.addShiftNote(
        facilityId = facility.id,
        wingId = wing.wing.id,
        shiftKey = "night",
        shiftDate = LocalDate.now().toString(),
        authorId = "nurse_nocturno",
        kind = ShiftNoteKind.SHIFT_SUMMARY,
        body = "Turno nocturno sin incidentes. Residentes estables."
    )
    println("  ✓ Shift note added")

    // 10. List shift notes
    println("\n═══ Step 10: List Shift Notes ═══")
    val shiftNotes = hub.care.shiftNotes(facility.id, LocalDate.now().toString())
    println("  📋 Shift notes: ${shiftNotes.size}")

    // 11. Night shift ends
    println("\n═══ Step 11: Night Shift Ends ═══")
    println("  🌅 Night shift ended at ${Instant.now()}")

    // 12. Summary
    println("\n═══════════════════════════════════════════")
    println("  SHIFT HANDOVER COMPLETE")
    println("═══════════════════════════════════════════")
    println("  Rounds: ${rounds.size}")
    println("  Care notes: 1")
    println("  Resident notes: 1")
    println("  Shift notes: ${shiftNotes.size}")
    println("═══════════════════════════════════════════")
}
