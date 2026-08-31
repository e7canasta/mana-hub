package com.hub.panel.seed

import java.sql.Connection
import java.sql.DriverManager

/**
 * El piso mínimo para arrancar: José, su cama, y su perfil de observación.
 *
 * Correr: `./gradlew :panel-api:runSeed` (o esta `main` desde el IDE).
 *
 * ── Por qué es tan chico ────────────────────────────────────
 *
 * El seed anterior inventaba seis residentes, tres episodios y estados de cama.
 * Eso hacía tres daños que costaron encontrar:
 *
 *  1. **Ponía a José en `bed-1`**, y `bed-4` era de Ana. El censo del motor
 *     (`night-watch-runtime/profiles/census.json`) dice `jose → bed-4`, así que
 *     todo lo que el simulador emitía para esa cama pertenecía, según el Hub, a
 *     otra residente. Los episodios salían con la habitación equivocada.
 *
 *  2. **Fabricaba episodios con `Instant.now() - N días`.** Los tres quedaban
 *     con el mismo segundo exacto —la hora de arranque del Hub— mientras los del
 *     motor caen en tiempo simulado. Dos escalas de tiempo en la misma pantalla.
 *
 *  3. **Sembraba `current_bed_states`**, que es una observación. El estado de
 *     una persona no se inventa: lo produce el motor. Sembrarlo hacía que un
 *     residente sin monitor pareciera monitoreado.
 *
 * La regla que sigue este archivo: **se siembra la instalación, no la historia**.
 * Camas, habitaciones, personas y su configuración inicial son hechos
 * administrativos que alguien cargó. Episodios, estados y observaciones los
 * produce el sistema, y si no hay, la respuesta correcta es que no haya — que es
 * exactamente lo que el panel ahora sabe mostrar.
 *
 * Un residente y una cama alcanzan porque el censo tiene uno. Cuando el censo
 * crezca, crece esto y en el mismo orden.
 */
fun main() {
    val url = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/mana_hub"
    val user = System.getenv("DB_USER") ?: "postgres"
    val pass = System.getenv("DB_PASS") ?: "postgres"

    DriverManager.getConnection(url, user, pass).use { conn ->
        conn.autoCommit = false
        try {
            seedFacility(conn)
            seedWing(conn)
            seedRoom(conn)
            seedBed(conn)
            seedResident(conn)
            seedAssignment(conn)
            seedObservationProfile(conn)
            conn.commit()
            println("\n✓ Piso mínimo listo: José García en la 301, cama bed-4, perfil de observación.")
            println("  El resto —episodios, estados, observaciones— lo produce el motor.")
        } catch (e: Exception) {
            conn.rollback()
            println("✗ Falló y se revirtió entero: ${e.message}")
            throw e
        }
    }
}

private fun seedFacility(conn: Connection) {
    conn.prepareStatement(
        """INSERT INTO facilities (id, name, timezone, created_at, updated_at, version)
           VALUES ('fac-001', 'Residencia Los Robles', 'America/Argentina/Buenos_Aires', now(), now(), 0)
           ON CONFLICT (id) DO NOTHING"""
    ).use { it.executeUpdate() }
    println("  ✓ residencia")
}

private fun seedWing(conn: Connection) {
    conn.prepareStatement(
        """INSERT INTO wings (id, facility_id, name, floor, sort_order, created_at, updated_at, version)
           VALUES ('wing-a', 'fac-001', 'Ala Norte', 'Bajo', 0, now(), now(), 0)
           ON CONFLICT (id) DO NOTHING"""
    ).use { it.executeUpdate() }
    println("  ✓ ala")
}

/* La 301 y no la 101: es la habitación que el censo nombra en `night-jose-301`,
 * y la que el catálogo DAG del motor usa como escenario de referencia. */
private fun seedRoom(conn: Connection) {
    conn.prepareStatement(
        """INSERT INTO rooms (id, wing_id, number, created_at, updated_at, version)
           VALUES ('room-301', 'wing-a', '301', now(), now(), 0)
           ON CONFLICT (id) DO NOTHING"""
    ).use { it.executeUpdate() }
    println("  ✓ habitación 301")
}

/* `bed-4` con monitor `m1`, los dos tal como los nombra el censo. Si estos dos
 * identificadores no coinciden con los del motor, el Hub recibe eventos de una
 * cama que no conoce y los atribuye mal — sin fallar. */
private fun seedBed(conn: Connection) {
    conn.prepareStatement(
        """INSERT INTO beds (id, room_id, label, monitor_key, created_at, updated_at, version)
           VALUES ('bed-4', 'room-301', 'Cama A', 'm1', now(), now(), 0)
           ON CONFLICT (id) DO NOTHING"""
    ).use { it.executeUpdate() }
    println("  ✓ cama bed-4 (monitor m1)")
}

private fun seedResident(conn: Connection) {
    conn.prepareStatement(
        """INSERT INTO residents (id, full_name, birth_date, admission_date, status, created_at, updated_at, version)
           VALUES ('jose', 'José García', '1942-03-15'::date, '2024-01-15'::date, 'active', now(), now(), 0)
           ON CONFLICT (id) DO NOTHING"""
    ).use { it.executeUpdate() }
    println("  ✓ residente jose")
}

private fun seedAssignment(conn: Connection) {
    conn.prepareStatement(
        """INSERT INTO resident_bed_assignments (id, resident_id, bed_id, starts_at, created_at, version)
           VALUES ('assign-jose', 'jose', 'bed-4', now(), now(), 0)
           ON CONFLICT (id) DO NOTHING"""
    ).use { it.executeUpdate() }
    println("  ✓ jose → bed-4")
}

/**
 * El perfil de arranque: observar.
 *
 * Nivel bajo, sin apoyo de movilidad, sin autopilot y en modo preset — o sea,
 * ninguna regla apartada del catálogo. Es el punto de partida honesto para un
 * residente del que todavía no se sabe nada: el sistema mira y no habla, y el
 * director decide qué encender.
 *
 * El seed anterior lo ponía en `HIGH` con autopilot y modo `CUSTOM`, que es
 * afirmar una decisión clínica que nadie tomó.
 */
private fun seedObservationProfile(conn: Connection) {
    conn.prepareStatement(
        """INSERT INTO alarm_profile_versions
           (id, resident_id, valid_from, risk_level, mobility_aid, autopilot, mode, template_id, updated_by, created_at, version)
           VALUES ('profile-jose-v1', 'jose', now(), 'LOW', 'NONE', false, 'PRESET', 'standard', 'seed', now(), 0)
           ON CONFLICT DO NOTHING"""
    ).use { it.executeUpdate() }
    println("  ✓ perfil de observación (bajo, preset, sin autopilot)")
}
