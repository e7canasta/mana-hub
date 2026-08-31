package com.hub.panel.command

import com.hub.shared.panel.*
import com.manahive.contracts.policy.MobilityAid
import com.manahive.contracts.policy.PolicyMode
import com.manahive.contracts.policy.RiskLevel
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.sql.Timestamp
import java.util.UUID

private fun riskLevelOf(v: String?): RiskLevel =
    v?.let { runCatching { RiskLevel.valueOf(it.uppercase()) }.getOrNull() } ?: RiskLevel.MEDIUM

private fun mobilityAidOf(v: String?): MobilityAid =
    v?.let { runCatching { MobilityAid.valueOf(it.uppercase()) }.getOrNull() } ?: MobilityAid.NONE

private fun policyModeOf(v: String?): PolicyMode =
    v?.let { runCatching { PolicyMode.valueOf(it.uppercase()) }.getOrNull() } ?: PolicyMode.PRESET

@Service("panelCommandService")
class PanelCommandService(private val jdbc: JdbcTemplate) {

    @Transactional
    fun reviewEpisode(
        episodeId: String,
        verdict: EpisodeVerdict,
        note: String?,
        actorId: String,
    ): ReviewEpisodeResponse {
        val id = UUID.randomUUID().toString()
        val now = Instant.now()
        val ts = Timestamp.from(now)

        jdbc.update(
            """
            INSERT INTO history_episode_reviews (id, episode_id, status, actor_id, detection_verdict, review_note, resolved_at)
            VALUES (?, ?, 'resolved', ?, ?, ?, ?)
            """,
            id, episodeId, actorId, verdict.name, note, ts,
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
        val now = Instant.now().toString()

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
        val now = Instant.now().toString()

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
        val now = Instant.now()
        val ts = Timestamp.from(now)

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
