package com.hub.panel.command

import com.hub.shared.panel.*
import com.manahive.contracts.policy.MobilityAid
import com.manahive.contracts.policy.PolicyMode
import com.manahive.contracts.policy.RiskLevel
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.hub.shared.time.HubClock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.sql.Timestamp
import java.util.UUID

private fun riskLevelOf(v: String?): RiskLevel =
    v?.let { runCatching { RiskLevel.valueOf(it.uppercase()) }.getOrNull() } ?: RiskLevel.MEDIUM

private fun mobilityAidOf(v: String?): MobilityAid =
    v?.let { runCatching { MobilityAid.valueOf(it.uppercase()) }.getOrNull() } ?: MobilityAid.NONE

private fun policyModeOf(v: String?): PolicyMode =
    v?.let { runCatching { PolicyMode.valueOf(it.uppercase()) }.getOrNull() } ?: PolicyMode.PRESET

@Service("panelCommandService")
class PanelCommandService(
    private val jdbc: JdbcTemplate,
    /* El reloj del Hub, no `Instant.now()`.
     *
     * Todo lo que esta clase escribe —la revisión de un episodio, una nota, una
     * versión de perfil— lleva la hora del Hub. Durante un escenario ese reloj
     * está en manual y en la misma escala que hive, así que la revisión de un
     * episodio del 3 de septiembre cae el 3 de septiembre y no el día en que
     * alguien corrió la prueba. En producción es el reloj del sistema y no
     * cambia nada.
     *
     * Antes acá había dos líneas de tiempo en la misma base: los eventos con la
     * hora simulada y las revisiones con la real. Un episodio ocurrido después
     * de haber sido revisado. */
    private val clock: HubClock,
) {

    @Transactional
    fun reviewEpisode(
        episodeId: String,
        verdict: EpisodeVerdict,
        note: String?,
        actorId: String,
    ): ReviewEpisodeResponse {
        val id = UUID.randomUUID().toString()
        val now = clock.now()
        /* UTC explícito, no `Timestamp.from(now)`.
         *
         * Estas columnas son `timestamp` **sin zona**, y `Timestamp.from` liga
         * el valor convertido a la zona del JVM: escribía hora local. Medido
         * contra la base — las filas viejas quedaron en UTC (14:15) y las nuevas
         * en local (11:30), y el `ORDER BY resolved_at DESC` que decide cuál es
         * la última revisión elegía las viejas. O sea: reclasificar un episodio
         * no cambiaba lo que la cola mostraba.
         *
         * Y eso depende de cómo esté configurado el JVM que escribe, así que dos
         * instancias del mismo Hub podían intercalar sus filas de forma
         * incomparable. La hora de la base tiene que ser una sola. */
        val ts = Timestamp.valueOf(LocalDateTime.ofInstant(now, ZoneOffset.UTC))

        jdbc.update(
            """
            INSERT INTO history_episode_reviews (id, episode_id, status, actor_id, detection_verdict, review_note, resolved_at)
            VALUES (?, ?, 'resolved', ?, ?, ?, ?)
            """,
            /* `.value` y no `.name`.
             *
             * El enum se guardaba con su nombre (`NEAR_MISS`) y se lee con
             * `EpisodeVerdict.from`, que compara contra `.value` (`near_miss`) y
             * cae en `CONFIRMED` cuando no encuentra. Resultado medido contra el
             * Hub corriendo: los **tres** veredictos se releían como CONFIRMED.
             *
             * En un producto de detección de caídas ese es el peor sentido
             * posible del error: un falso positivo pasaba a caída confirmada,
             * inflando el conteo de doce meses y reseteando la racha — las dos
             * métricas con las que el director juzga a su equipo.
             *
             * Es el mismo formato que ya escribe `PATCH /history-episodes/{id}`,
             * que sí hacía round-trip correcto. */
            id, episodeId, actorId, verdict.value, note, ts,
        )

        jdbc.update(
            """
            UPDATE episodes SET status = 'resolved', status_actor_id = ?, status_at = ?, updated_at = ?
            WHERE id = ?
            """,
            actorId, ts, ts, episodeId,
        )

        jdbc.update(
            """
            INSERT INTO episode_transitions (id, episode_id, from_status, to_status, actor_id, occurred_at, sequence)
            VALUES (?, ?, 'pending', 'resolved', ?, ?,
                    COALESCE((SELECT MAX(sequence) + 1 FROM episode_transitions WHERE episode_id = ?), 1))
            """,
            UUID.randomUUID().toString(), episodeId, actorId, ts, episodeId,
        )

        return ReviewEpisodeResponse(
            id = id,
            episodeId = episodeId,
            verdict = verdict,
            reviewNote = note,
            reviewedBy = actorId,
            reviewedAt = now.toString(),
        )
    }

    @Transactional
    fun createNote(
        episodeId: String,
        kind: NoteKind,
        body: String,
        authorId: String,
    ): NoteCreatedResponse {
        val id = UUID.randomUUID().toString()
        val now = clock.now().toString()

        jdbc.update(
            """
            INSERT INTO episode_notes (id, episode_id, author_id, kind, body, timestamp, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            id, episodeId, authorId, kind.name, body, now, now,
        )

        return NoteCreatedResponse(
            id = id,
            episodeId = episodeId,
            residentId = null,
            kind = kind,
            body = body,
            authorId = authorId,
            createdAt = now,
        )
    }

    @Transactional
    fun createResidentNote(
        residentId: String,
        kind: NoteKind,
        body: String,
        authorId: String,
    ): NoteCreatedResponse {
        val id = UUID.randomUUID().toString()
        val now = clock.now().toString()

        jdbc.update(
            """
            INSERT INTO resident_notes (id, resident_id, author_id, kind, body, timestamp, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            id, residentId, authorId, kind.name, body, now, now, now,
        )

        return NoteCreatedResponse(
            id = id,
            episodeId = null,
            residentId = residentId,
            kind = kind,
            body = body,
            authorId = authorId,
            createdAt = now,
        )
    }

    @Transactional
    fun savePreferences(
        residentId: String,
        riskLevel: RiskLevel?,
        mobilityAid: MobilityAid?,
        autopilot: Boolean?,
        mode: PolicyMode?,
        templateId: String?,
        overrides: Map<TransitionId, TransitionOverrideDto>?,
        reason: String?,
        updatedBy: String?,
    ): SavePreferencesResponse {
        val now = clock.now()
        /* UTC explícito, no `Timestamp.from(now)`.
         *
         * Estas columnas son `timestamp` **sin zona**, y `Timestamp.from` liga
         * el valor convertido a la zona del JVM: escribía hora local. Medido
         * contra la base — las filas viejas quedaron en UTC (14:15) y las nuevas
         * en local (11:30), y el `ORDER BY resolved_at DESC` que decide cuál es
         * la última revisión elegía las viejas. O sea: reclasificar un episodio
         * no cambiaba lo que la cola mostraba.
         *
         * Y eso depende de cómo esté configurado el JVM que escribe, así que dos
         * instancias del mismo Hub podían intercalar sus filas de forma
         * incomparable. La hora de la base tiene que ser una sola. */
        val ts = Timestamp.valueOf(LocalDateTime.ofInstant(now, ZoneOffset.UTC))

        data class CurrentProfile(
            val riskLevel: RiskLevel, val mobilityAid: MobilityAid, val autopilot: Boolean,
            val mode: PolicyMode, val templateId: String?,
        )
        val current = jdbc.queryForObject(
            """
            SELECT risk_level, mobility_aid, autopilot, mode, template_id
            FROM alarm_profile_versions WHERE resident_id = ? AND valid_to IS NULL
            """,
            { rs, _ ->
                CurrentProfile(
                    riskLevel = riskLevelOf(rs.getString("risk_level")),
                    mobilityAid = mobilityAidOf(rs.getString("mobility_aid")),
                    autopilot = rs.getBoolean("autopilot"),
                    mode = policyModeOf(rs.getString("mode")),
                    templateId = rs.getString("template_id"),
                )
            },
            residentId,
        )

        jdbc.update(
            "UPDATE alarm_profile_versions SET valid_to = ? WHERE resident_id = ? AND valid_to IS NULL",
            ts, residentId,
        )

        val id = UUID.randomUUID().toString()
        jdbc.update(
            """
            INSERT INTO alarm_profile_versions
                (id, resident_id, valid_from, risk_level, mobility_aid, autopilot, mode, template_id, updated_by, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            id, residentId, ts,
            (riskLevel ?: current.riskLevel).name,
            (mobilityAid ?: current.mobilityAid).name,
            autopilot ?: current.autopilot,
            (mode ?: current.mode).name,
            templateId ?: current.templateId,
            updatedBy, ts,
        )

        return SavePreferencesResponse(
            id = id,
            residentId = residentId,
            riskLevel = riskLevel ?: current.riskLevel,
            mobilityAid = mobilityAid ?: current.mobilityAid,
            autopilot = autopilot ?: current.autopilot,
            updatedAt = now.toString(),
        )
    }
}
