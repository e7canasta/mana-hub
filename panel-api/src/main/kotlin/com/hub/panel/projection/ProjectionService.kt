package com.hub.panel.projection

import com.hub.panel.dto.*
import com.hub.shared.domain.BedLocation
import com.manahive.contracts.policy.MobilityAid
import com.manahive.contracts.policy.PolicyMode
import com.manahive.contracts.policy.RiskLevel
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private fun riskLevelOf(v: String?): RiskLevel =
    v?.let { runCatching { RiskLevel.valueOf(it.uppercase()) }.getOrNull() } ?: RiskLevel.MEDIUM

private fun mobilityAidOf(v: String?): MobilityAid =
    v?.let { runCatching { MobilityAid.valueOf(it.uppercase()) }.getOrNull() } ?: MobilityAid.NONE

private fun policyModeOf(v: String?): PolicyMode =
    v?.let { runCatching { PolicyMode.valueOf(it.uppercase()) }.getOrNull() } ?: PolicyMode.PRESET

/**
 * Projectiones del panel vía JDBC directo.
 *
 * DUPLICACIÓN CON [com.hub.views.ProjectionService]:
 * - **Residente rail**: ambos resuelven bed→room→wing. Este servicio lo hace
 *   vía SQL JOINs; el otro vía JPA con cache por llamada. Ambos producen el
 *   mismo modelo conceptual ([com.hub.shared.domain.BedLocation]).
 * - **Episodios**: este servicio usa SQL lateral para el último review y
 *   closed_at; el otro usa repos JPA + lógica en Kotlin.
 * - **Alarm preferences**: este servicio lee alarm_profile_versions vía JDBC;
 *   el otro vía AlarmProfileRepository JPA.
 *
 * La consolidación futura debería unificar las proyecciones en un solo módulo,
 * eliminando el panel-api o migrándolo a usar los repos JPA del dominio.
 */
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

    /** DUPLICACIÓN: bed→room→wing JOIN — mismo patrón que LocationResolver en bootstrap. */
    private val residentRailMapper = RowMapper { rs, _ ->
        ResidentRailDto(
            id = rs.getString("id"),
            fullName = rs.getString("full_name"),
            location = BedLocation(
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

    /* La ventana del episodio: desde `occurred_at`, hasta la señal de cierre.
     *
     * Las dos puntas son **hora de hive**, que es la que vale: el momento en que
     * el evento dice que se generó, no el momento en que el Hub lo recibió ni
     * cuándo alguien lo revisó. `episodes.status_at` no sirve para esto —es
     * cuándo el Hub cambió el estado, que durante un escenario cae en otra
     * escala— y por eso `closedAt` venía en `null` fijo.
     *
     * Con la ventana explícita, los eventos son del episodio por **cuándo
     * pasaron** y no por un enlace que alguien tenga que mantener. Eso se apoya
     * en una premisa del dominio: no hay más de un episodio abierto por
     * residente a la vez, así que un instante pertenece a lo sumo a uno. Si esa
     * premisa cayera, la atribución por ventana deja de ser sana — vale decirlo
     * acá, que es donde alguien la va a leer.
     *
     * El veredicto es **el último**, no uno cualquiera.
     *
     * Era un `LEFT JOIN history_episode_reviews` a secas: un episodio revisado
     * dos veces devolvía dos filas del mismo episodio y el veredicto que
     * apareciera primero, sin orden definido. Reclasificar —que es exactamente
     * lo que un director hace cuando se equivoca— dejaba la cola mostrando el
     * juicio viejo, o los dos.
     *
     * `LEFT JOIN LATERAL ... LIMIT 1` deja una fila por episodio y toma la
     * revisión más reciente, que es la que vale. */
    fun episodeFeed(): EpisodeFeedDto {
        val episodes = jdbc.query(
            """
            SELECT e.id, e.resident_id, r.full_name as resident_name,
                   w.name as wing_name, rm.number as room_number,
                   e.severity, e.title, e.occurred_at, e.status,
                   clo.closed_at,
                   hed.kind, hed.injury_status, hed.narrative,
                   her.detection_verdict as verdict
            FROM episodes e
            JOIN residents r ON r.id = e.resident_id
            LEFT JOIN resident_bed_assignments rba ON rba.resident_id = r.id AND rba.ends_at IS NULL
            LEFT JOIN beds b ON b.id = rba.bed_id
            LEFT JOIN rooms rm ON rm.id = b.room_id
            LEFT JOIN wings w ON w.id = rm.wing_id
            LEFT JOIN history_episode_detections hed ON hed.source_episode_id = e.id
            LEFT JOIN LATERAL (
                SELECT detection_verdict
                FROM history_episode_reviews
                WHERE episode_id = e.id
                /* Ordena por `resolved_at`, que es el reloj del dominio, y eso
                 * tiene un filo conocido: si una revisión se escribió con el
                 * reloj en manual y quedó en el futuro, gana para siempre sobre
                 * las que se escriban después en tiempo real — y reclasificar
                 * deja de tener efecto visible.
                 *
                 * La corrección es `V18__review_recorded_at.sql`: una columna
                 * monótona puesta por la base (`DEFAULT now()`), para separar
                 * "cuándo pasó" de "cuándo se registró". No se usa todavía
                 * porque `spring.flyway.enabled` está en `false` y las
                 * migraciones de este repo no corren solas; ordenar por una
                 * columna que la base no tiene rompe la consulta entera.
                 *
                 * Mientras el reloj sea coherente en cada corrida, esto anda. */
                ORDER BY resolved_at DESC
                LIMIT 1
            ) her ON true
            LEFT JOIN LATERAL (
                SELECT timestamp AS closed_at
                FROM sentinel_signals
                WHERE episode_id = e.id AND type = 'EPISODE_CLOSED'
                ORDER BY timestamp DESC
                LIMIT 1
            ) clo ON true
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
            location = BedLocation(
                wingName = rs.getString("wing_name"),
                roomNumber = rs.getString("room_number"),
                bedLabel = null,
            ),
            severity = EpisodeSeverity.from(rs.getString("severity")),
            kind = rs.getString("kind") ?: "other",
            title = rs.getString("title") ?: rs.getString("kind") ?: "Episodio",
            openedAt = rs.getString("occurred_at") ?: rs.getTimestamp("occurred_at")?.toInstant()?.toString() ?: "",
            closedAt = rs.getString("closed_at"),
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
                       e.occurred_at, e.escalation_level, clo.closed_at,
                       hed.kind, hed.injury_status, hed.self_recovery, hed.narrative, hed.response_seconds,
                       her.detection_verdict as verdict
                FROM episodes e
                JOIN residents r ON r.id = e.resident_id
                LEFT JOIN history_episode_detections hed ON hed.source_episode_id = e.id
                LEFT JOIN LATERAL (
                SELECT detection_verdict
                FROM history_episode_reviews
                WHERE episode_id = e.id
                -- Ver la nota en episodeFeed: ordena por el reloj del dominio,
                -- con el filo conocido de que una revisión escrita en hora
                -- simulada gana para siempre. La corrección propuesta es
                -- V18__review_recorded_at.sql.
                ORDER BY resolved_at DESC
                LIMIT 1
            ) her ON true
            LEFT JOIN LATERAL (
                SELECT timestamp AS closed_at
                FROM sentinel_signals
                WHERE episode_id = e.id AND type = 'EPISODE_CLOSED'
                ORDER BY timestamp DESC
                LIMIT 1
            ) clo ON true
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
                        closedAt = rs.getString("closed_at"),
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
            /* Las revisiones se piden **después**, no adentro del `RowMapper`.
             *
             * Una consulta anidada mientras el `ResultSet` de la de afuera sigue
             * abierto explota, y el `catch` de abajo la convertía en `null` — o
             * sea en un 404. El endpoint decía "este episodio no existe" cuando
             * en realidad la consulta se había roto: el peor mensaje posible,
             * porque manda a buscar el problema al lado equivocado. */
            ?.let { it.copy(reviews = episodeReviews(episodeId)) }
        } catch (e: Exception) {
            /* Se registra antes de devolver null. Sin esto, cualquier error de
             * SQL en esta función se presenta al cliente como un 404 y no queda
             * rastro de qué pasó — que es exactamente cómo se perdió una hora
             * acá. */
            log.error("episodeDetail({}) falló: {}", episodeId, e.message, e)
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
                        location = BedLocation(
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
                location = BedLocation(
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

    /**
     * Las revisiones del episodio, la ultima primero en la lectura.
     *
     * Venia `emptyList()` fijo, y eso rompia el ciclo completo de clasificar:
     * el panel guardaba, releia el detalle, no encontraba veredicto y mostraba
     * "sin revision registrada". El director veia que su decision no quedaba —
     * y volvia a clasificar, generando otra fila.
     *
     * `from` compara contra el valor en minuscula; durante un tiempo el comando
     * guardo el `name` del enum y todo se leia como CONFIRMED. Las filas viejas
     * con ese formato siguen ahi, asi que el `lowercase()` no es decorativo.
     */
    fun episodeReviews(episodeId: String): List<ReviewDto> = jdbc.query(
        """
        SELECT id, actor_id, detection_verdict, review_note, resolved_at
        FROM history_episode_reviews
        WHERE episode_id = ?
        ORDER BY resolved_at
        """,
        { rs, _ ->
            ReviewDto(
                id = rs.getString("id"),
                actorId = rs.getString("actor_id"),
                verdict = rs.getString("detection_verdict")
                    ?.let { EpisodeVerdict.from(it.lowercase()) },
                reviewNote = rs.getString("review_note"),
                reviewedAt = rs.getTimestamp("resolved_at")?.toInstant()?.toString(),
            )
        },
        episodeId,
    )

    companion object {
        private val log = LoggerFactory.getLogger(PanelProjectionService::class.java)
    }

}
