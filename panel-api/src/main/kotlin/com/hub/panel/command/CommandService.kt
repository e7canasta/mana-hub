package com.hub.panel.command

import com.hub.care.application.dto.CreateEpisodeNoteRequest
import com.hub.care.application.dto.CreateResidentNoteRequest
import com.hub.care.application.service.NoteApplicationService
import com.hub.history.application.dto.ReviewHistoryEpisodeRequest
import com.hub.history.application.service.HistoryEpisodeApplicationService
import com.hub.policy.application.dto.UpdateAlarmProfileRequest
import com.hub.policy.application.service.AlarmProfileApplicationService
import com.hub.panel.dto.*
import com.manahive.contracts.policy.MobilityAid
import com.manahive.contracts.policy.PolicyMode
import com.manahive.contracts.policy.RiskLevel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Panel command service — delega a los application services del dominio.
 *
 * Fowler: "La fuente de verdad para cada writes es el application service
 * de su bounded context. El panel es un BFF que orquesta, no un segundo
 * sistema de persistencia".
 *
 * Antes esto usaba JdbcTemplate para escribir directo en tablas de otros
 * bounded contexts (history, care, policy). Ahora delega a los services
 * que encapsulan la lógica de negocio y los domain events.
 */
@Service("panelCommandService")
class PanelCommandService(
    private val historyService: HistoryEpisodeApplicationService,
    private val noteService: NoteApplicationService,
    private val alarmProfileService: AlarmProfileApplicationService,
) {

    @Transactional
    fun reviewEpisode(
        episodeId: String,
        verdict: EpisodeVerdict,
        note: String?,
        actorId: String,
    ): ReviewEpisodeResponse {
        val review = historyService.reviewHistoryEpisode(
            episodeId = episodeId,
            request = ReviewHistoryEpisodeRequest(
                status = "resolved",
                actorId = actorId,
                detectionVerdict = verdict.value,
                reviewNote = note,
            )
        )

        if (note != null) {
            noteService.createEpisodeNote(
                CreateEpisodeNoteRequest(
                    episodeId = episodeId,
                    authorId = actorId,
                    kind = com.hub.care.domain.model.EpisodeNoteKind.CLINICAL_NOTE,
                    body = note,
                )
            )
        }

        return ReviewEpisodeResponse(
            id = review.id,
            episodeId = episodeId,
            verdict = verdict,
            reviewNote = note,
            reviewedBy = actorId,
            reviewedAt = review.resolvedAt?.toString() ?: "",
        )
    }

    @Transactional
    fun createNote(
        episodeId: String,
        kind: NoteKind,
        body: String,
        authorId: String,
    ): NoteCreatedResponse {
        val domainKind = try {
            com.hub.care.domain.model.EpisodeNoteKind.from(kind.name)
        } catch (_: Exception) {
            com.hub.care.domain.model.EpisodeNoteKind.CLINICAL_NOTE
        }

        val response = noteService.createEpisodeNote(
            CreateEpisodeNoteRequest(
                episodeId = episodeId,
                authorId = authorId,
                kind = domainKind,
                body = body,
            )
        )

        return NoteCreatedResponse(
            id = response.id,
            episodeId = episodeId,
            residentId = null,
            kind = kind,
            body = body,
            authorId = authorId,
            createdAt = response.createdAt.toString(),
        )
    }

    @Transactional
    fun createResidentNote(
        residentId: String,
        kind: NoteKind,
        body: String,
        authorId: String,
    ): NoteCreatedResponse {
        val domainKind = try {
            com.hub.care.domain.model.ResidentNoteKind.from(kind.name)
        } catch (_: Exception) {
            com.hub.care.domain.model.ResidentNoteKind.CARE
        }

        val response = noteService.createResidentNote(
            CreateResidentNoteRequest(
                residentId = residentId,
                authorId = authorId,
                kind = domainKind,
                body = body,
            )
        )

        return NoteCreatedResponse(
            id = response.id,
            episodeId = null,
            residentId = residentId,
            kind = kind,
            body = body,
            authorId = authorId,
            createdAt = response.createdAt.toString(),
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
        val response = alarmProfileService.updateResidentProfile(
            residentId = residentId,
            request = UpdateAlarmProfileRequest(
                riskLevel = riskLevel?.name?.lowercase(),
                mobilityAid = mobilityAid?.name?.lowercase(),
                autopilot = autopilot,
                mode = mode?.name?.lowercase(),
                templateId = templateId,
                overridesJson = null,
                reason = reason,
                updatedBy = updatedBy,
            )
        )

        return SavePreferencesResponse(
            id = residentId,
            residentId = residentId,
            riskLevel = riskLevelOf(response.profile.riskLevel),
            mobilityAid = mobilityAidOf(response.profile.mobilityAid),
            autopilot = response.profile.autopilot,
            updatedAt = response.profile.updatedAt ?: "",
        )
    }

    private fun riskLevelOf(v: String?): RiskLevel =
        v?.let { runCatching { RiskLevel.valueOf(it.uppercase()) }.getOrNull() } ?: RiskLevel.MEDIUM

    private fun mobilityAidOf(v: String?): MobilityAid =
        v?.let { runCatching { MobilityAid.valueOf(it.uppercase()) }.getOrNull() } ?: MobilityAid.NONE
}
