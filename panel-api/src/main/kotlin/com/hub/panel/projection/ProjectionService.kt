package com.hub.panel.projection

import com.hub.shared.panel.*
import com.manahive.contracts.policy.MobilityAid
import com.manahive.contracts.policy.PolicyMode
import com.manahive.contracts.policy.RiskLevel
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service

private fun riskLevelOf(v: String?): RiskLevel =
    v?.let { runCatching { RiskLevel.valueOf(it.uppercase()) }.getOrNull() } ?: RiskLevel.MEDIUM

private fun mobilityAidOf(v: String?): MobilityAid =
    v?.let { runCatching { MobilityAid.valueOf(it.uppercase()) }.getOrNull() } ?: MobilityAid.NONE

private fun policyModeOf(v: String?): PolicyMode =
    v?.let { runCatching { PolicyMode.valueOf(it.uppercase()) }.getOrNull() } ?: PolicyMode.PRESET

@Service("panelProjectionService")
class PanelProjectionService(private val jdbc: JdbcTemplate) {

    // ─── Residentes ─────────────────────────────────────────────

    fun residentRail(): List<ResidentRailDto> = jdbc.query(
        """
        SELECT r.id, r.full_name,
               w.name as wing_name, rm.number as room_number, b.label as bed_label,
               cbs.state, cbs.staff_present, cbs.state_since
        FROM residents r
        LEFT JOIN resident_bed_assignments rba ON rba.resident_id = r.id AND rba.ends_at IS NULL
        LEFT JOIN beds b ON b.id = rba.bed_id
        LEFT JOIN rooms rm ON rm.id = b.room_id
        LEFT JOIN wings w ON w.id = rm.wing_id
        LEFT JOIN current_bed_states cbs ON cbs.bed_id = b.id
        WHERE r.status = 'active'
        ORDER BY r.full_name
        """,
        residentRailMapper,
    )

    private val residentRailMapper = RowMapper { rs, _ ->
        ResidentRailDto(
            id = rs.getString("id"),
            fullName = rs.getString("full_name"),
            location = LocationDto(
                wingName = rs.getString("wing_name"),
                roomNumber = rs.getString("room_number"),
                bedLabel = rs.getString("bed_label"),
            ),
            currentState = CurrentStateDto(
                state = rs.getString("state"),
                staffPresent = rs.getBoolean("staff_present"),
                stateSince = rs.getString("state_since"),
            ),
        )
    }

    fun residentDetail(residentId: String): ResidentRailDto? = jdbc.queryForObject(
        """
        SELECT r.id, r.full_name,
               w.name as wing_name, rm.number as room_number, b.label as bed_label,
               cbs.state, cbs.staff_present, cbs.state_since
        FROM residents r
        LEFT JOIN resident_bed_assignments rba ON rba.resident_id = r.id AND rba.ends_at IS NULL
        LEFT JOIN beds b ON b.id = rba.bed_id
        LEFT JOIN rooms rm ON rm.id = b.room_id
        LEFT JOIN wings w ON w.id = rm.wing_id
        LEFT JOIN current_bed_states cbs ON cbs.bed_id = b.id
        WHERE r.id = ?
        """,
        residentRailMapper,
        residentId,
    )

    // ─── Episodios ──────────────────────────────────────────────

    fun episodeFeed(): EpisodeFeedDto {
        val episodes = jdbc.query(
            """
            SELECT e.id, e.resident_id, r.full_name as resident_name,
                   w.name as wing_name, rm.number as room_number,
                   e.severity, e.title, e.occurred_at, e.status,
                   hed.kind, hed.injury_status, hed.narrative,
                   her.detection_verdict as verdict
            FROM episodes e
            JOIN residents r ON r.id = e.resident_id
            LEFT JOIN resident_bed_assignments rba ON rba.resident_id = r.id AND rba.ends_at IS NULL
            LEFT JOIN beds b ON b.id = rba.bed_id
            LEFT JOIN rooms rm ON rm.id = b.room_id
            LEFT JOIN wings w ON w.id = rm.wing_id
            LEFT JOIN history_episode_detections hed ON hed.source_episode_id = e.id
            LEFT JOIN history_episode_reviews her ON her.episode_id = e.id
            ORDER BY e.occurred_at DESC
            """,
            episodeListItemMapper,
        )

        val pending = episodes.count { it.verdict == null }
        val injured = episodes.count { it.injury == "minor" || it.injury == "major" }

        return EpisodeFeedDto(
            episodes = episodes,
            summary = EpisodeSummaryDto(pending = pending, injured = injured, total = episodes.size),
        )
    }

    private val episodeListItemMapper = RowMapper { rs, _ ->
        EpisodeListItemDto(
            id = rs.getString("id"),
            residentId = rs.getString("resident_id"),
            residentName = rs.getString("resident_name"),
            location = LocationDto(
                wingName = rs.getString("wing_name"),
                roomNumber = rs.getString("room_number"),
                bedLabel = null,
            ),
            severity = EpisodeSeverity.from(rs.getString("severity")),
            kind = rs.getString("kind") ?: "other",
            title = rs.getString("title") ?: rs.getString("kind") ?: "Episodio",
            openedAt = rs.getString("occurred_at") ?: rs.getTimestamp("occurred_at")?.toInstant()?.toString() ?: "",
            closedAt = null,
            verdict = rs.getString("verdict")?.let { EpisodeVerdict.from(it) },
            injury = rs.getString("injury_status"),
        )
    }

    fun episodeDetail(episodeId: String): EpisodeDetailDto? {
        return try {
            jdbc.queryForObject(
                """
                SELECT e.id, e.resident_id, r.full_name as resident_name,
                       e.severity, e.status, e.title, e.detail,
                       e.occurred_at, e.escalation_level,
                       hed.kind, hed.injury_status, hed.self_recovery, hed.narrative, hed.response_seconds,
                       her.detection_verdict as verdict
                FROM episodes e
                JOIN residents r ON r.id = e.resident_id
                LEFT JOIN history_episode_detections hed ON hed.source_episode_id = e.id
                LEFT JOIN history_episode_reviews her ON her.episode_id = e.id
                WHERE e.id = ?
                """,
                { rs, _ ->
                    EpisodeDetailDto(
                        id = rs.getString("id"),
                        residentId = rs.getString("resident_id"),
                        residentName = rs.getString("resident_name"),
                        severity = EpisodeSeverity.from(rs.getString("severity")),
                        status = EpisodeStatus.from(rs.getString("status")),
                        kind = rs.getString("kind") ?: "other",
                        title = rs.getString("title"),
                        detail = rs.getString("detail"),
                        narrative = rs.getString("narrative"),
                        occurredAt = rs.getString("occurred_at") ?: rs.getTimestamp("occurred_at")?.toInstant()?.toString() ?: "",
                        closedAt = null,
                        injury = rs.getString("injury_status"),
                        selfRecovery = rs.getBoolean("self_recovery"),
                        responseSeconds = rs.getInt("response_seconds").takeIf { !rs.wasNull() },
                        escalationLevel = rs.getInt("escalation_level"),
                        timeline = emptyList(),
                        reviews = emptyList(),
                        interventions = emptyList(),
                        notes = emptyList(),
                    )
                },
                episodeId,
            )
        } catch (_: Exception) {
            null
        }
    }

    // ─── Preferencias ───────────────────────────────────────────

    fun preferenceFull(residentId: String): PreferenceFullDto? {
        return try {
            jdbc.queryForObject(
                """
                SELECT apv.resident_id, r.full_name as resident_name,
                       w.name as wing_name, rm.number as room_number, b.label as bed_label,
                       apv.risk_level, apv.mobility_aid, apv.autopilot,
                       apv.mode, apv.template_id,
                       apv.valid_from as updated_at, apv.updated_by
                FROM alarm_profile_versions apv
                JOIN residents r ON r.id = apv.resident_id
                LEFT JOIN resident_bed_assignments rba ON rba.resident_id = r.id AND rba.ends_at IS NULL
                LEFT JOIN beds b ON b.id = rba.bed_id
                LEFT JOIN rooms rm ON rm.id = b.room_id
                LEFT JOIN wings w ON w.id = rm.wing_id
                WHERE apv.resident_id = ? AND apv.valid_to IS NULL
                """,
                { rs, _ ->
                    PreferenceFullDto(
                        residentId = rs.getString("resident_id"),
                        residentName = rs.getString("resident_name"),
                        location = LocationDto(
                            wingName = rs.getString("wing_name"),
                            roomNumber = rs.getString("room_number"),
                            bedLabel = rs.getString("bed_label"),
                        ),
                        riskLevel = riskLevelOf(rs.getString("risk_level")),
                        mobilityAid = mobilityAidOf(rs.getString("mobility_aid")),
                        autopilot = rs.getBoolean("autopilot"),
                        mode = policyModeOf(rs.getString("mode")),
                        templateId = rs.getString("template_id"),
                        overrides = emptyMap(),
                        recommendation = null,
                        updatedAt = rs.getString("updated_at"),
                        updatedBy = rs.getString("updated_by"),
                    )
                },
                residentId,
            )
        } catch (_: Exception) {
            null
        }
    }

    fun preferenceList(): List<PreferenceListItemDto> = jdbc.query(
        """
        SELECT apv.resident_id, r.full_name as resident_name,
               w.name as wing_name, rm.number as room_number, b.label as bed_label,
               apv.risk_level, apv.mobility_aid, apv.autopilot,
               apv.mode,
               apv.valid_from as updated_at, apv.updated_by
        FROM alarm_profile_versions apv
        JOIN residents r ON r.id = apv.resident_id
        LEFT JOIN resident_bed_assignments rba ON rba.resident_id = r.id AND rba.ends_at IS NULL
        LEFT JOIN beds b ON b.id = rba.bed_id
        LEFT JOIN rooms rm ON rm.id = b.room_id
        LEFT JOIN wings w ON w.id = rm.wing_id
        WHERE apv.valid_to IS NULL
        ORDER BY r.full_name
        """,
        { rs, _ ->
            PreferenceListItemDto(
                residentId = rs.getString("resident_id"),
                residentName = rs.getString("resident_name"),
                location = LocationDto(
                    wingName = rs.getString("wing_name"),
                    roomNumber = rs.getString("room_number"),
                    bedLabel = rs.getString("bed_label"),
                ),
                riskLevel = riskLevelOf(rs.getString("risk_level")),
                mobilityAid = mobilityAidOf(rs.getString("mobility_aid")),
                autopilot = rs.getBoolean("autopilot"),
                mode = policyModeOf(rs.getString("mode")),
                dayActiveCount = 0,
                nightActiveCount = 0,
                recommendation = null,
                updatedAt = rs.getString("updated_at"),
                updatedBy = rs.getString("updated_by"),
            )
        },
    )
}
