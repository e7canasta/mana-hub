package com.hub.clients.blueprint.scenarios

import com.hub.clients.core.manahub
import java.time.LocalDate

fun main() {
    val hub = manahub("http://localhost:8080") {}

    println("═══════════════════════════════════════════")
    println("  BLUEPRINT: Resident Onboarding")
    println("  Context Group: Resident Lifecycle")
    println("═══════════════════════════════════════════\n")

    // 1. Register admin user
    println("═══ Step 1: Register Admin User ═══")
    val admin = hub.identity.registerOwner(
        username = "admin_onboarding",
        displayName = "Admin Onboarding",
        password = "admin123"
    )
    println("  ✓ User: ${admin.username} (${admin.role})")

    // 2. Create facility hierarchy
    println("\n═══ Step 2: Create Facility Hierarchy ═══")
    val facility = hub.residence.setupFacility("Residencia Esperanza") {
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
    println("  ✓ Facility: ${facility.name} (${facility.id})")

    val tree = facility.tree()
    println("  ✓ Wings: ${tree.wings.size}")
    tree.wings.forEach { wingDef ->
        println("    └─ ${wingDef.wing.name}: ${wingDef.rooms.size} rooms")
        wingDef.rooms.forEach { roomDef ->
            println("       └─ Room ${roomDef.room.number}: ${roomDef.beds.size} beds")
        }
    }

    // 3. Admit resident
    println("\n═══ Step 3: Admit Resident ═══")
    val resident = hub.population.admitResident(
        fullName = "María García López",
        birthDate = LocalDate.of(1935, 3, 15),
        admissionDate = LocalDate.now()
    )
    println("  ✓ Resident: ${resident.fullName} (${resident.id})")
    println("  ✓ Status: ${resident.status}")
    println("  ✓ Birth: ${resident.birthDate}")
    println("  ✓ Admission: ${resident.admissionDate}")

    // 4. Assign to bed
    println("\n═══ Step 4: Assign to Bed ═══")
    val bed = facility.firstBed()
    if (bed != null) {
        val assignment = resident.assignTo(bed.id)
        println("  ✓ Assigned to: ${bed.id}")
        println("  ✓ Assignment open: ${assignment.isOpen}")

        // Verify assignment
        val assignments = resident.assignments()
        println("  ✓ Total assignments: ${assignments.size}")
    } else {
        println("  ✗ No beds available")
    }

    // 5. Summary
    println("\n═══════════════════════════════════════════")
    println("  RESIDENT ONBOARDING COMPLETE")
    println("═══════════════════════════════════════════")
    println("  Resident: ${resident.fullName}")
    println("  Facility: ${facility.name}")
    println("  Bed: ${bed?.id ?: "Not assigned"}")
    println("  Status: ${resident.status}")
    println("═══════════════════════════════════════════")
}
