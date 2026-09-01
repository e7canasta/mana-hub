package com.hub.care.application.service

import com.hub.care.application.dto.*
import com.hub.care.domain.model.*
import com.hub.care.domain.repository.RoundRepository
import com.hub.care.domain.repository.RoundTaskRepository
import com.hub.shared.domain.DomainEventPublisher
import com.hub.shared.domain.WingId
import com.hub.shared.domain.publishAndClear
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RoundApplicationService(
    private val roundRepository: RoundRepository,
    private val roundTaskRepository: RoundTaskRepository,
    private val eventPublisher: DomainEventPublisher
) {

    @Transactional
    fun createRound(request: CreateRoundRequest): RoundResponse {
        val wingId = WingId(request.wingId)
        val existing = roundRepository.findInProgressByWingId(wingId)
        require(existing == null) { "There is already an in-progress round for this wing" }
        val round = Round.create(wingId = wingId, scheduledFor = request.scheduledFor)
        val saved = roundRepository.save(round)
        eventPublisher.publishAndClear(round)
        return saved.toResponse()
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
        val completed = round.complete(actorId)
        val saved = roundRepository.save(completed)
        eventPublisher.publishAndClear(completed)
        return saved.toResponse()
    }

    @Transactional
    fun updateRoundTask(taskId: String, request: UpdateRoundTaskRequest): RoundTaskResponse {
        val task = roundTaskRepository.findById(RoundTaskId(taskId))
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
