package com.hub.panel.seed

import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/**
 * Seed data para demo del panel.
 * Correr: ./gradlew :panel-api:compileKotlin && java -cp "panel-api/build/classes/kotlin/main:..." com.hub.panel.seed.SeedDataKt
 * O directamente desde IntelliJ.
 */
fun main() {
    val url = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/mana_hub"
    val user = System.getenv("DB_USER") ?: "postgres"
    val pass = System.getenv("DB_PASS") ?: "postgres"

    DriverManager.getConnection(url, user, pass).use { conn ->
        conn.autoCommit = false
        try {
            seedFacilities(conn)
            seedWings(conn)
            seedRooms(conn)
            seedBeds(conn)
            seedResidents(conn)
            seedAssignments(conn)
            seedBedStates(conn)
            seedAlarmProfiles(conn)
            seedEpisodes(conn)
            seedDetections(conn)
            seedNotes(conn)
            conn.commit()
            println("✓ Seed completo")
        } catch (e: Exception) {
            conn.rollback()
            println("✗ Rollback: ${e.message}")
            throw e
        }
    }
}

private fun String.q() = "'${this.replace("'", "''")}'"

private fun seedFacilities(conn: Connection) {
    conn.prepareStatement(
        """INSERT INTO facilities (id, name, timezone, created_at, updated_at, version)
           VALUES (?, ?, 'America/Argentina/Buenos_Aires', now(), now(), 0)
           ON CONFLICT (id) DO NOTHING"""
    ).use { ps ->
        ps.setString(1, "fac-001")
        ps.setString(2, "Residencia Los Robles")
        ps.executeUpdate()
    }
    println("  ✓ facilities")
}

private fun seedWings(conn: Connection) {
    val wings = listOf(
        "wing-a" to "Ala A - Piso Bajo",
        "wing-b" to "Ala B - Piso Alto",
    )
    conn.prepareStatement(
        """INSERT INTO wings (id, facility_id, name, floor, sort_order, created_at, updated_at, version)
           VALUES (?, 'fac-001', ?, ?, 0, now(), now(), 0)
           ON CONFLICT (id) DO NOTHING"""
    ).use { ps ->
        wings.forEach { (id, name) ->
            ps.setString(1, id)
            ps.setString(2, name)
            ps.setString(3, if (id == "wing-a") "Bajo" else "Alto")
            ps.addBatch()
        }
        ps.executeBatch()
    }
    println("  ✓ wings")
}

private fun seedRooms(conn: Connection) {
    data class Room(val id: String, val wingId: String, val number: String)
    val rooms = listOf(
        Room("room-101", "wing-a", "101"),
        Room("room-102", "wing-a", "102"),
        Room("room-103", "wing-a", "103"),
        Room("room-104", "wing-a", "104"),
        Room("room-201", "wing-b", "201"),
        Room("room-202", "wing-b", "202"),
    )
    conn.prepareStatement(
        """INSERT INTO rooms (id, wing_id, number, created_at, updated_at, version)
           VALUES (?, ?, ?, now(), now(), 0)
           ON CONFLICT (id) DO NOTHING"""
    ).use { ps ->
        rooms.forEach { r ->
            ps.setString(1, r.id)
            ps.setString(2, r.wingId)
            ps.setString(3, r.number)
            ps.addBatch()
        }
        ps.executeBatch()
    }
    println("  ✓ rooms")
}

private fun seedBeds(conn: Connection) {
    data class Bed(val id: String, val roomId: String, val label: String, val monitorKey: String)
    val beds = listOf(
        Bed("bed-1", "room-101", "Cama 1", "cam-001"),
        Bed("bed-2", "room-102", "Cama 1", "cam-002"),
        Bed("bed-3", "room-103", "Cama 1", "cam-003"),
        Bed("bed-4", "room-104", "Cama 1", "cam-004"),
        Bed("bed-5", "room-201", "Cama 1", "cam-005"),
        Bed("bed-6", "room-202", "Cama 1", "cam-006"),
    )
    conn.prepareStatement(
        """INSERT INTO beds (id, room_id, label, monitor_key, created_at, updated_at, version)
           VALUES (?, ?, ?, ?, now(), now(), 0)
           ON CONFLICT (id) DO NOTHING"""
    ).use { ps ->
        beds.forEach { b ->
            ps.setString(1, b.id)
            ps.setString(2, b.roomId)
            ps.setString(3, b.label)
            ps.setString(4, b.monitorKey)
            ps.addBatch()
        }
        ps.executeBatch()
    }
    println("  ✓ beds")
}

private fun seedResidents(conn: Connection) {
    data class Resident(val id: String, val fullName: String, val birthDate: String)
    val residents = listOf(
        Resident("jose",      "José García",       "1942-03-15"),
        Resident("maria",     "María López",       "1938-07-22"),
        Resident("carlos",    "Carlos Martínez",   "1945-11-08"),
        Resident("ana",       "Ana Rodríguez",     "1940-01-30"),
        Resident("pedro",     "Pedro Sánchez",     "1936-09-12"),
        Resident("lucia",     "Lucía Fernández",   "1943-05-18"),
        Resident("roberto",   "Roberto Díaz",      "1941-12-03"),
        Resident("elena",     "Elena Moreno",      "1939-08-25"),
    )
    conn.prepareStatement(
        """INSERT INTO residents (id, full_name, birth_date, admission_date, status, created_at, updated_at, version)
           VALUES (?, ?, ?::date, '2024-01-15'::date, 'active', now(), now(), 0)
           ON CONFLICT (id) DO NOTHING"""
    ).use { ps ->
        residents.forEach { r ->
            ps.setString(1, r.id)
            ps.setString(2, r.fullName)
            ps.setString(3, r.birthDate)
            ps.addBatch()
        }
        ps.executeBatch()
    }
    println("  ✓ residents (${residents.size})")
}

private fun seedAssignments(conn: Connection) {
    val assignments = listOf(
        "jose"   to "bed-1",
        "maria"  to "bed-2",
        "carlos" to "bed-3",
        "ana"    to "bed-4",
        "pedro"  to "bed-5",
        "lucia"  to "bed-6",
    )
    conn.prepareStatement(
        """INSERT INTO resident_bed_assignments (id, resident_id, bed_id, starts_at, created_at, version)
           VALUES (?, ?, ?, now(), now(), 0)
           ON CONFLICT (id) DO NOTHING"""
    ).use { ps ->
        assignments.forEach { (residentId, bedId) ->
            ps.setString(1, "assign-${residentId}")
            ps.setString(2, residentId)
            ps.setString(3, bedId)
            ps.addBatch()
        }
        ps.executeBatch()
    }
    println("  ✓ assignments (${assignments.size})")
}

private fun seedBedStates(conn: Connection) {
    val now = Instant.now()
    val states = listOf(
        Triple("bed-1", "lying", false),
        Triple("bed-2", "sitting_in_bed", true),
        Triple("bed-3", "lying", false),
        Triple("bed-4", "out_of_bed", false),
        Triple("bed-5", "lying", false),
        Triple("bed-6", "sitting_in_bed", true),
    )
    conn.prepareStatement(
        """INSERT INTO current_bed_states (bed_id, state, staff_present, state_since, updated_at)
           VALUES (?, ?, ?, ?, ?)
           ON CONFLICT (bed_id) DO UPDATE SET state = ?, staff_present = ?, state_since = ?, updated_at = ?"""
    ).use { ps ->
        states.forEach { (bedId, state, staff) ->
            ps.setString(1, bedId)
            ps.setString(2, state)
            ps.setBoolean(3, staff)
            ps.setTimestamp(4, java.sql.Timestamp.from(now))
            ps.setTimestamp(5, java.sql.Timestamp.from(now))
            ps.setString(6, state)
            ps.setBoolean(7, staff)
            ps.setTimestamp(8, java.sql.Timestamp.from(now))
            ps.setTimestamp(9, java.sql.Timestamp.from(now))
            ps.addBatch()
        }
        ps.executeBatch()
    }
    println("  ✓ bed_states")
}

private fun seedAlarmProfiles(conn: Connection) {
    data class Profile(val residentId: String, val riskLevel: String, val mobilityAid: String, val autopilot: Boolean, val mode: String)
    val profiles = listOf(
        Profile("jose",   "HIGH",   "WALKER",     true,  "CUSTOM"),
        Profile("maria",  "MEDIUM", "NONE",       true,  "PRESET"),
        Profile("carlos", "LOW",    "NONE",       true,  "PRESET"),
        Profile("ana",    "HIGH",   "WHEELCHAIR", false, "CUSTOM"),
        Profile("pedro",  "MEDIUM", "WALKER",     true,  "PRESET"),
        Profile("lucia",  "LOW",    "NONE",       true,  "PRESET"),
    )
    conn.prepareStatement(
        """INSERT INTO alarm_profile_versions
           (id, resident_id, valid_from, risk_level, mobility_aid, autopilot, mode, updated_by, created_at, version)
           VALUES (?, ?, now(), ?, ?, ?, ?, 'seed', now(), 0)
           ON CONFLICT DO NOTHING"""
    ).use { ps ->
        profiles.forEach { p ->
            ps.setString(1, "profile-${p.residentId}")
            ps.setString(2, p.residentId)
            ps.setString(3, p.riskLevel)
            ps.setString(4, p.mobilityAid)
            ps.setBoolean(5, p.autopilot)
            ps.setString(6, p.mode)
            ps.addBatch()
        }
        ps.executeBatch()
    }
    println("  ✓ alarm_profiles (${profiles.size})")
}

private fun seedEpisodes(conn: Connection) {
    data class Episode(val id: String, val residentId: String, val severity: String, val title: String, val detail: String, val occurredAt: String)
    val episodes = listOf(
        Episode("ep-001", "jose",   "critical",  "Caída en baño",            "Residente encontrado en el suelo del baño",      "2026-08-30T14:23:00Z"),
        Episode("ep-002", "ana",    "warning",   "Salida de cama nocturna",  "Se levantó de la cama a las 03:15",              "2026-08-30T06:15:00Z"),
        Episode("ep-003", "maria",  "info",      "Permanencia en baño",      "32 minutos en baño, personal confirmó OK",       "2026-08-29T11:00:00Z"),
        Episode("ep-004", "jose",   "warning",   "Salida de habitación",     "Deambulación por pasillo sin andador",           "2026-08-29T16:45:00Z"),
        Episode("ep-005", "pedro",  "critical",  "Caída en habitación",      "Caida al levantarse de la silla",                "2026-08-28T09:30:00Z"),
    )
    conn.prepareStatement(
        """INSERT INTO episodes (id, resident_id, severity, status, title, detail, occurred_at, escalation_level, created_at, updated_at, version)
           VALUES (?, ?, ?, 'pending', ?, ?, ?::timestamp, 0, now(), now(), 0)
           ON CONFLICT (id) DO NOTHING"""
    ).use { ps ->
        episodes.forEach { ep ->
            ps.setString(1, ep.id)
            ps.setString(2, ep.residentId)
            ps.setString(3, ep.severity)
            ps.setString(4, ep.title)
            ps.setString(5, ep.detail)
            ps.setString(6, ep.occurredAt)
            ps.addBatch()
        }
        ps.executeBatch()
    }
    println("  ✓ episodes (${episodes.size})")
}

private fun seedDetections(conn: Connection) {
    data class Detection(val id: String, val episodeId: String, val residentId: String, val kind: String, val severity: String, val occurredAt: String, val injuryStatus: String?, val narrative: String)
    val detections = listOf(
        Detection("det-001", "ep-001", "jose",  "fall",       "critical",  "2026-08-30T14:23:00Z", "minor",  "Caída detectada por cámara en baño. Residente en suelo, consciente."),
        Detection("det-002", "ep-002", "ana",   "bed_exit",   "warning",   "2026-08-30T06:15:00Z", null,    "Salida de cama detectada a las 03:15. Sin caída asociada."),
        Detection("det-003", "ep-003", "maria", "bathroom_dwell", "info", "2026-08-29T11:00:00Z", null,    "Permanencia prolongada en baño. Personal contactado y confirmó OK."),
        Detection("det-004", "ep-004", "jose",  "room_exit",  "warning",   "2026-08-29T16:45:00Z", null,    "Residente salió de habitación sin andador. Deambulación por pasillo."),
        Detection("det-005", "ep-005", "pedro", "fall",       "critical",  "2026-08-28T09:30:00Z", "minor",  "Caída al levantarse de silla en habitación."),
    )
    conn.prepareStatement(
        """INSERT INTO history_episode_detections
           (id, source_record_id, resident_id, source_episode_id, kind, severity, occurred_at, injury_status, self_recovery, narrative, source, created_at, version)
           VALUES (?, ?, ?, ?, ?, ?, ?::timestamp, ?, false, ?, 'camera', now(), 0)
           ON CONFLICT (id) DO NOTHING"""
    ).use { ps ->
        detections.forEach { d ->
            ps.setString(1, d.id)
            ps.setString(2, "src-${d.id}")
            ps.setString(3, d.residentId)
            ps.setString(4, d.episodeId)
            ps.setString(5, d.kind)
            ps.setString(6, d.severity)
            ps.setString(7, d.occurredAt)
            ps.setString(8, d.injuryStatus)
            ps.setString(9, d.narrative)
            ps.addBatch()
        }
        ps.executeBatch()
    }
    println("  ✓ detections (${detections.size})")
}

private fun seedNotes(conn: Connection) {
    conn.prepareStatement(
        """INSERT INTO episode_notes (id, episode_id, author_id, kind, body, timestamp, created_at)
           VALUES (?, ?, ?, ?, ?, ?, ?)
           ON CONFLICT (id) DO NOTHING"""
    ).use { ps ->
        val now = Instant.now().toString()
        ps.setString(1, "enote-001")
        ps.setString(2, "ep-001")
        ps.setString(3, "enfermera_ana")
        ps.setString(4, "CLINICAL_NOTE")
        ps.setString(5, "Residente con corte superficial en rodilla izquierda. Limpieza y curación realizada.")
        ps.setString(6, now)
        ps.setString(7, now)
        ps.executeUpdate()
    }

    conn.prepareStatement(
        """INSERT INTO resident_notes (id, resident_id, author_id, kind, body, timestamp, created_at, updated_at)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?)
           ON CONFLICT (id) DO NOTHING"""
    ).use { ps ->
        val now = Instant.now().toString()
        ps.setString(1, "rnote-001")
        ps.setString(2, "jose")
        ps.setString(3, "doctor_lopez")
        ps.setString(4, "INSIGHT")
        ps.setString(5, "Patrón de 3 caídas en 14 días. Evaluar ajuste de medicación y revisión de entorno.")
        ps.setString(6, now)
        ps.setString(7, now)
        ps.setString(8, now)
        ps.executeUpdate()
    }

    conn.prepareStatement(
        """INSERT INTO resident_notes (id, resident_id, author_id, kind, body, timestamp, created_at, updated_at)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?)
           ON CONFLICT (id) DO NOTHING"""
    ).use { ps ->
        val now = Instant.now().toString()
        ps.setString(1, "rnote-002")
        ps.setString(2, "ana")
        ps.setString(3, "enfermera_maria")
        ps.setString(4, "OBSERVATION")
        ps.setString(5, "Residente coopera con rutina nocturna. Solicita ayuda para ir al baño.")
        ps.setString(6, now)
        ps.setString(7, now)
        ps.setString(8, now)
        ps.executeUpdate()
    }
    println("  ✓ notes")
}
