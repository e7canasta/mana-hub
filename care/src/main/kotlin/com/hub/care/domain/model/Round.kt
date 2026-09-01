package com.hub.care.domain.model

import com.hub.shared.domain.AggregateRoot
import com.hub.shared.domain.WingId
import com.hub.care.domain.event.RoundEvent
import java.time.Instant

@ConsistentCopyVisibility
data class Round private constructor(
    override val id: RoundId,
    val wingId: WingId,
    val status: RoundStatus,
    val scheduledFor: Instant?,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val startedBy: String?,
    val completedBy: String?,
    override var version: Long
) : AggregateRoot<RoundId>() {

    val isInProgress: Boolean get() = status == RoundStatus.IN_PROGRESS
    val isCompleted: Boolean get() = status == RoundStatus.COMPLETED

    fun start(actorId: String): Round {
        require(isInProgress) { "Round is not in progress" }
        val next = copy(
            status = RoundStatus.IN_PROGRESS,
            startedAt = Instant.now(),
            startedBy = actorId,
            version = version + 1
        )
        next._domainEvents.add(
            RoundEvent.Started(roundId = id, wingId = wingId, actorId = actorId)
        )
        return next
    }

    fun complete(actorId: String): Round {
        require(isInProgress) { "Round is not in progress" }
        val next = copy(
            status = RoundStatus.COMPLETED,
            completedAt = Instant.now(),
            completedBy = actorId,
            version = version + 1
        )
        next._domainEvents.add(
            RoundEvent.Completed(roundId = id, wingId = wingId, actorId = actorId)
        )
        return next
    }

    fun cancel(): Round {
        require(isInProgress) { "Round is not in progress" }
        val next = copy(
            status = RoundStatus.CANCELLED,
            version = version + 1
        )
        next._domainEvents.add(
            RoundEvent.Cancelled(roundId = id, wingId = wingId)
        )
        return next
    }

    companion object {
        fun create(wingId: WingId, scheduledFor: Instant?): Round {
            val round = Round(
                id = RoundId.random(), wingId = wingId, status = RoundStatus.IN_PROGRESS,
                scheduledFor = scheduledFor, startedAt = null, completedAt = null,
                startedBy = null, completedBy = null, version = 0
            )
            round._domainEvents.add(
                RoundEvent.Started(roundId = round.id, wingId = wingId, actorId = "system")
            )
            return round
        }

        fun reconstitute(
            id: RoundId, wingId: WingId, status: RoundStatus, scheduledFor: Instant?,
            startedAt: Instant?, completedAt: Instant?, startedBy: String?,
            completedBy: String?, version: Long
        ): Round = Round(id, wingId, status, scheduledFor, startedAt, completedAt, startedBy, completedBy, version)
    }
}
