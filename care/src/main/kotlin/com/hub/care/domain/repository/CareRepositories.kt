package com.hub.care.domain.repository

import com.hub.care.domain.model.*
import com.hub.population.domain.model.ResidentId
import com.hub.residence.domain.model.WingId

interface RoundRepository {
    fun findById(id: RoundId): Round?
    fun findByWingId(wingId: WingId): List<Round>
    fun findInProgressByWingId(wingId: WingId): Round?
    fun save(round: Round): Round
}

interface RoundTaskRepository {
    fun findByRoundId(roundId: RoundId): List<RoundTask>
    fun findById(id: RoundId): RoundTask?
    fun save(task: RoundTask): RoundTask
}

interface CareNoteRepository {
    fun findByResidentId(residentId: ResidentId): List<CareNote>
    fun save(note: CareNote): CareNote
}
