package com.hub.clients

import com.hub.clients.core.manahub
import com.hub.clients.identity.Role
import java.time.LocalDate

fun main() {
    println("=== ManaHub DSL v2 — Domain-First ===\n")

    manahub("http://localhost:8080") {

        // ── Identity: register users with domain roles ──
        val owner = identity.registerOwner(
            username = "owner-${System.currentTimeMillis()}",
            displayName = "Dr. Garcia"
        )
        println("Registered: $owner")

        val supervisor = identity.registerSupervisor(
            username = "sup-${System.currentTimeMillis()}",
            displayName = "Supervisor Lopez"
        )
        println("Registered: $supervisor")

        val staff = identity.registerStaff(
            username = "staff-${System.currentTimeMillis()}",
            displayName = "Enfermera Ruiz"
        )
        println("Registered: $staff")

        // Full builder syntax
        val admin = identity.registerUser {
            username = "admin-${System.currentTimeMillis()}"
            displayName = "Admin Root"
            role = Role.OWNER
            password = "supersecret"
            jobTitle = "Director General"
        }
        println("Registered: $admin (${admin.jobTitle})")

        println("Users: ${queryUsers().size}")

        // ── Residence: declarative hierarchy ──
        val facility = residence.setupFacility("Residencia DSL v2 ${System.currentTimeMillis()}") {
            timezone = "America/Argentina/Buenos_Aires"

            wing("Ala Norte") {
                floor = "1"

                room("101") {
                    roomType = "individual"
                    bed("Cama 1")
                    bed("Cama 2")
                }
                room("102") {
                    roomType = "individual"
                    bed("Cama 1")
                }
            }
            wing("Ala Sur") {
                floor = "2"
                sortOrder = 1

                room("201") {
                    roomType = "doble"
                    bed("Cama A")
                    bed("Cama B")
                }
            }
        }
        println("\nFacility: $facility")

        val tree = facility.tree()
        println("Hierarchy:")
        tree.wings.forEach { wingTree ->
            println("  ├─ ${wingTree.wing.name} (floor ${wingTree.wing.floor})")
            wingTree.rooms.forEach { roomTree ->
                println("  │  ├─ Room ${roomTree.room.number} [${roomTree.room.roomType}]")
                roomTree.beds.forEach { bed ->
                    println("  │  │  └─ ${bed.label}")
                }
            }
        }

        // ── Population: admit residents and assign beds ──
        val resident1 = population.admitResident {
            fullName = "Maria Fernandez"
            birthDate = LocalDate.of(1940, 3, 15)
            admissionDate = LocalDate.now().minusDays(30)
        }
        println("\nAdmitted: $resident1")

        val resident2 = population.admitResident(
            fullName = "Carlos Mendez",
            birthDate = LocalDate.of(1935, 7, 22)
        )
        println("Admitted: $resident2")

        // Assign to first bed using domain helper
        val bed = facility.firstBed()!!
        val assignment = resident1.assignTo(bed.id)
        println("Assigned: ${resident1.fullName} → ${bed.label} (open=${assignment.isOpen})")

        println("Assignments: ${resident1.assignments().size}")
        println("All beds: ${queryBeds().size}")
        println("All residents: ${queryResidents().size}")
    }

    println("\n=== DSL v2 Test PASSED ===")
}
