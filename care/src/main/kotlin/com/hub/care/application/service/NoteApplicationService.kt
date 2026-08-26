package com.hub.care.application.service

import com.hub.care.application.dto.*
import com.hub.care.domain.model.*
import com.hub.care.domain.repository.EpisodeNoteRepository
import com.hub.care.domain.repository.ResidentNoteRepository
import com.hub.care.domain.repository.ShiftNoteRepository
import com.hub.population.domain.model.ResidentId
import com.hub.shared.domain.Identifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NoteApplicationService(
    private val residentNoteRepository: ResidentNoteRepository,
    private val episodeNoteRepository: EpisodeNoteRepository,
    private val shiftNoteRepository: ShiftNoteRepository
) {

    @Transactional
    fun createResidentNote(request: CreateResidentNoteRequest): ResidentNoteResponse {
        val note = ResidentNote.create(
            residentId = ResidentId(request.residentId),
            authorId = request.authorId,
            kind = request.kind,
            body = request.body,
            sourceEventId = request.sourceEventId,
            timestamp = request.timestamp
        )
        return residentNoteRepository.save(note).toResponse()
    }

    @Transactional(readOnly = true)
    fun getResidentNotes(residentId: String): List<ResidentNoteResponse> {
        return residentNoteRepository.findByResidentId(ResidentId(residentId)).map { it.toResponse() }
    }

    @Transactional
    fun createEpisodeNote(request: CreateEpisodeNoteRequest): EpisodeNoteResponse {
        val note = EpisodeNote.create(
            episodeId = request.episodeId,
            authorId = request.authorId,
            kind = request.kind,
            body = request.body,
            timestamp = request.timestamp
        )
        return episodeNoteRepository.save(note).toResponse()
    }

    @Transactional(readOnly = true)
    fun getEpisodeNotes(episodeId: String): List<EpisodeNoteResponse> {
        return episodeNoteRepository.findByEpisodeId(episodeId).map { it.toResponse() }
    }

    @Transactional
    fun createShiftNote(request: CreateShiftNoteRequest): ShiftNoteResponse {
        val note = ShiftNote.create(
            facilityId = request.facilityId,
            wingId = request.wingId,
            shiftKey = request.shiftKey,
            shiftDate = request.shiftDate,
            authorId = request.authorId,
            kind = request.kind,
            body = request.body,
            timestamp = request.timestamp
        )
        return shiftNoteRepository.save(note).toResponse()
    }

    @Transactional(readOnly = true)
    fun getShiftNotes(facilityId: String, shiftDate: String): List<ShiftNoteResponse> {
        return shiftNoteRepository.findByFacilityAndDate(facilityId, shiftDate).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getWingShiftNotes(wingId: String, shiftDate: String): List<ShiftNoteResponse> {
        return shiftNoteRepository.findByWingAndDate(wingId, shiftDate).map { it.toResponse() }
    }

    private fun ResidentNote.toResponse() = ResidentNoteResponse(
        id = id.value, residentId = residentId.value, authorId = authorId,
        kind = kind, body = body, sourceEventId = sourceEventId,
        timestamp = timestamp, createdAt = createdAt
    )

    private fun EpisodeNote.toResponse() = EpisodeNoteResponse(
        id = id.value, episodeId = episodeId, authorId = authorId,
        kind = kind, body = body, timestamp = timestamp, createdAt = createdAt
    )

    private fun ShiftNote.toResponse() = ShiftNoteResponse(
        id = id.value, facilityId = facilityId, wingId = wingId,
        shiftKey = shiftKey, shiftDate = shiftDate, authorId = authorId,
        kind = kind, body = body, timestamp = timestamp, createdAt = createdAt
    )
}
