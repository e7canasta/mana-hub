package com.hub.care.application.service

import com.hub.care.application.dto.*
import com.hub.care.domain.model.*
import com.hub.care.domain.repository.RoundRepository
import com.hub.care.domain.repository.RoundTaskRepository
import com.hub.residence.domain.model.WingId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RoundApplicationService(
    private val roundRepository: RoundRepository,
    private val roundTaskRepository: RoundTaskRepository
) {

    @Transactional
    fun createRound(request: CreateRoundRequest): RoundResponse {
        val wingId = WingId(request.wingId)
        val existing = roundRepository.findInProgressByWingId(wingId)
        require(existing == null) { "There is already an in-progress round for this wing" }
        val round = Round.create(wingId = wingId, scheduledFor = request.scheduledFor)
        return roundRepository.save(round).toResponse()
    }

    @Transactional(readOnly = true)
    fun getCurrentRound(wingId: String): RoundResponse? {
        return roundRepository.findInProgressByWingId(WingId(wingId))?.toResponse()
    }

    @Transactional(readOnly = true)
    fun getRound(id: String): RoundResponse {
        return roundRepository.findById(RoundId(id))?.toResponse()
            ?: throw IllegalArgumentException("Round not found: $id")
    }

    @Transactional(readOnly = true)
    fun listRounds(wingId: String): List<RoundResponse> {
        return roundRepository.findByWingId(WingId(wingId)).map { it.toResponse() }
    }

    @Transactional
    fun updateRound(id: String, actorId: String): RoundResponse {
        val round = roundRepository.findById(RoundId(id))
            ?: throw IllegalArgumentException("Round not found: $id")
        return roundRepository.save(round.complete(actorId)).toResponse()
    }

    @Transactional
    fun updateRoundTask(taskId: String, request: UpdateRoundTaskRequest): RoundTaskResponse {
        val task = roundTaskRepository.findById(RoundId(taskId))
            ?: throw IllegalArgumentException("Task not found: $taskId")
        return roundTaskRepository.save(task.complete(request.note, request.completedBy ?: "system")).toTaskResponse()
    }

    private fun Round.toResponse() = RoundResponse(
        id = id.value, wingId = wingId.value, status = status, scheduledFor = scheduledFor,
        startedAt = startedAt, completedAt = completedAt, startedBy = startedBy, completedBy = completedBy
    )

    private fun RoundTask.toTaskResponse() = RoundTaskResponse(
        id = id.value, roundId = roundId.value, residentId = residentId.value,
        bedId = bedId?.value, status = status, note = note, completedAt = completedAt, completedBy = completedBy
    )
}
