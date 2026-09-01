package com.hub.care.domain.repository

import com.hub.care.domain.model.*
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.WingId

interface RoundRepository {
    fun findById(id: RoundId): Round?
    fun findByWingId(wingId: WingId): List<Round>
    fun findInProgressByWingId(wingId: WingId): Round?
    fun save(round: Round): Round
}

interface RoundTaskRepository {
    fun findByRoundId(roundId: RoundId): List<RoundTask>
    fun findById(id: RoundTaskId): RoundTask?
    fun save(task: RoundTask): RoundTask
}

interface CareNoteRepository {
    fun findByResidentId(residentId: ResidentId): List<CareNote>
    fun save(note: CareNote): CareNote
}
