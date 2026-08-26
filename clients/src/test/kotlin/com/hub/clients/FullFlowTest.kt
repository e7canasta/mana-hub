package com.hub.clients

import com.hub.clients.core.manahub
import com.hub.clients.identity.Role
import com.hub.clients.policy.RiskLevel
import com.hub.clients.streams.RegionType
import com.hub.clients.surveillance.EpisodeSeverity
import java.time.Instant
import java.time.LocalDate

fun main() {
    println("=== ManaHub Full Flow: Room → Stream → Region → Profile → Episode ===\n")

    manahub("http://localhost:8080") {

        // ── 1. Identity: register owner ──
        val owner = identity.registerOwner(
            username = "owner-${System.currentTimeMillis()}",
            displayName = "Dr. Garcia"
        )
        println("1. Owner: $owner")

        // ── 2. Residence: create facility with room ──
        val facility = residence.setupFacility("Residencia Monitoreo ${System.currentTimeMillis()}") {
            timezone = "America/Argentina/Buenos_Aires"

            wing("Ala Principal") {
                floor = "1"
                room("101") {
                    roomType = "individual"
                    bed("Cama 1")
                }
            }
        }
        println("2. Facility: $facility")

        val tree = facility.tree()
        val room = tree.wings.first().rooms.first().room
        val bed = tree.wings.first().rooms.first().beds.first()
        println("   Room: ${room.number}, Bed: ${bed.label}")

        // ── 3. Streams: assign video stream to room ──
        val stream = streams.assignStreamToRoom(room.id) {
            streamKey = "rtsp://camera-101.local/stream"
            name = "Camara Habitacion 101"
        }
        println("3. Stream: $stream")

        // ── 4. Streams: define monitoring regions ──
        val regions = stream.defineRegions {
            bed(points = "0,0,100,0,100,100,0,100", label = "Zona cama")
            hallway(points = "100,0,200,0,200,100,100,100", label = "Pasillo")
            exit(points = "200,0,300,0,300,50,200,50", label = "Salida")
            bathroom(points = "0,100,50,100,50,200,0,200", label = "Bano")
        }
        println("4. Regions defined: ${regions.size}")
        regions.forEach { println("   - ${it.regionType}: ${it.label}") }

        // ── 5. Population: admit resident ──
        val resident = population.admitResident {
            fullName = "Maria Fernandez"
            birthDate = LocalDate.of(1940, 3, 15)
            admissionDate = LocalDate.now().minusDays(30)
        }
        println("5. Resident: $resident")

        // Assign to bed
        resident.assignTo(bed.id)
        println("   Assigned to ${bed.label}")

        // ── 6. Policy: configure alarm profile ──
        val catalog = policy.catalog()
        println("6. Preset catalog: ${catalog.presets}")

        val profile = policy.configureAlarmProfile(resident.id) {
            templateId = "fall_risk"
            riskLevel = RiskLevel.HIGH
            autopilot = true
            mobilityAid = "walker"
            mode = "night_watch"
            updatedBy = owner.id
        }
        println("   Alarm profile: $profile")

        // ── 7. Surveillance: trigger episode ──
        val episode = surveillance.triggerEpisode(resident.id) {
            bedId = bed.id
            severity = EpisodeSeverity.WARNING
            title = "Movimiento detectado fuera de cama"
            detail = "Residente se levanto de la cama fuera del horario permitido"
            occurredAt = Instant.now()
            evidenceKind = "video_clip"
            evidenceRef = "rtsp://camera-101.local/clip-001"
        }
        println("7. Episode triggered: $episode")

        // ── 8. Surveillance: acknowledge episode ──
        val acknowledged = episode.acknowledge(owner.id)
        println("8. Acknowledged: status=${acknowledged.status}")

        // ── 9. Surveillance: resolve episode ──
        val resolved = episode.resolve("resolved")
        println("9. Resolved: status=${resolved.status}")

        // ── 10. Verify state ──
        val finalProfile = policy.alarmProfile(resident.id)
        val history = policy.alarmProfileHistory(resident.id)
        val pendingEpisodes = surveillance.pendingEpisodes()
        val streamRegions = stream.regions()

        println("\n10. Final state:")
        println("    Profile: ${finalProfile?.riskLevel} (current=${finalProfile?.isCurrent})")
        println("    Profile history: ${history.size} versions")
        println("    Pending episodes: ${pendingEpisodes.size}")
        println("    Stream regions: ${streamRegions.size}")
    }

    println("\n=== Full Flow Test PASSED ===")
}
