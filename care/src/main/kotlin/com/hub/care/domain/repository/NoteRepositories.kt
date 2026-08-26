package com.hub.care.domain.repository

import com.hub.care.domain.model.*
import com.hub.population.domain.model.ResidentId
import com.hub.shared.domain.Identifier

interface ResidentNoteRepository {
    fun findById(id: Identifier): ResidentNote?
    fun findByResidentId(residentId: ResidentId): List<ResidentNote>
    fun save(note: ResidentNote): ResidentNote
}

interface EpisodeNoteRepository {
    fun findById(id: Identifier): EpisodeNote?
    fun findByEpisodeId(episodeId: String): List<EpisodeNote>
    fun save(note: EpisodeNote): EpisodeNote
}

interface ShiftNoteRepository {
    fun findById(id: Identifier): ShiftNote?
    fun findByFacilityAndDate(facilityId: String, shiftDate: String): List<ShiftNote>
    fun findByWingAndDate(wingId: String, shiftDate: String): List<ShiftNote>
    fun save(note: ShiftNote): ShiftNote
}
