package com.hub.surveillance.domain.model

import com.hub.shared.domain.AggregateRoot
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.BedId
import java.time.Instant

class Episode private constructor(
    override val id: EpisodeId,
    val residentId: ResidentId,
    val bedId: BedId?,
    val evidenceKind: String?,
    val evidenceRef: String?,
    val ruleId: String?,
    val severity: EpisodeSeverity,
    val status: String,
    val statusActorId: String?,
    val statusAt: Instant?,
    val title: String?,
    val detail: String?,
    val occurredAt: Instant,
    val escalationLevel: Int,
    val escalatedAt: Instant?,
    val escalatedTo: String?,
    override var version: Long
) : AggregateRoot<EpisodeId>() {

    val isPending: Boolean get() = status == "pending"
    val isAcknowledged: Boolean get() = status == "acknowledged"
    val isResolved: Boolean get() = status == "resolved"

    fun acknowledge(actorId: String): Episode {
        require(isPending) { "Episode is not pending" }
        return reconstitute(
            id = id, residentId = residentId, bedId = bedId, evidenceKind = evidenceKind,
            evidenceRef = evidenceRef, ruleId = ruleId, severity = severity,
            status = "acknowledged", statusActorId = actorId, statusAt = Instant.now(),
            title = title, detail = detail, occurredAt = occurredAt,
            escalationLevel = escalationLevel, escalatedAt = escalatedAt,
            escalatedTo = escalatedTo, version = version + 1
        )
    }

    fun resolve(actorId: String): Episode {
        return reconstitute(
            id = id, residentId = residentId, bedId = bedId, evidenceKind = evidenceKind,
            evidenceRef = evidenceRef, ruleId = ruleId, severity = severity,
            status = "resolved", statusActorId = actorId, statusAt = Instant.now(),
            title = title, detail = detail, occurredAt = occurredAt,
            escalationLevel = escalationLevel, escalatedAt = escalatedAt,
            escalatedTo = escalatedTo, version = version + 1
        )
    }

    fun escalate(targetId: String): Episode {
        return reconstitute(
            id = id, residentId = residentId, bedId = bedId, evidenceKind = evidenceKind,
            evidenceRef = evidenceRef, ruleId = ruleId, severity = severity, status = status,
            statusActorId = statusActorId, statusAt = statusAt,
            title = title, detail = detail, occurredAt = occurredAt,
            escalationLevel = escalationLevel + 1, escalatedAt = Instant.now(),
            escalatedTo = targetId, version = version + 1
        )
    }

    /** Ventana: si llega señal más severa dentro del mismo episodio abierto, eleva severidad */
    fun elevateSeverity(newSeverity: EpisodeSeverity, newDetail: String?): Episode {
        if (!newSeverity.isMoreSevereThan(this.severity)) return this
        return reconstitute(
            id = id, residentId = residentId, bedId = bedId, evidenceKind = evidenceKind,
            evidenceRef = evidenceRef, ruleId = ruleId, severity = newSeverity, status = status,
            statusActorId = statusActorId, statusAt = statusAt,
            title = title, detail = newDetail ?: detail, occurredAt = occurredAt,
            escalationLevel = escalationLevel, escalatedAt = escalatedAt,
            escalatedTo = escalatedTo, version = version + 1
        )
    }

    companion object {
        fun create(
            residentId: ResidentId, bedId: BedId?, severity: EpisodeSeverity, title: String?,
            detail: String?, occurredAt: Instant, evidenceKind: String? = null, evidenceRef: String? = null
        ): Episode = Episode(
            id = EpisodeId.random(), residentId = residentId, bedId = bedId, evidenceKind = evidenceKind,
            evidenceRef = evidenceRef, ruleId = null, severity = severity, status = "pending",
            statusActorId = null, statusAt = null, title = title, detail = detail,
            occurredAt = occurredAt, escalationLevel = 0, escalatedAt = null, escalatedTo = null, version = 0
        )

        fun reconstitute(
            id: EpisodeId, residentId: ResidentId, bedId: BedId?, evidenceKind: String?,
            evidenceRef: String?, ruleId: String?, severity: EpisodeSeverity, status: String,
            statusActorId: String?, statusAt: Instant?, title: String?, detail: String?,
            occurredAt: Instant, escalationLevel: Int, escalatedAt: Instant?,
            escalatedTo: String?, version: Long
        ): Episode = Episode(
            id, residentId, bedId, evidenceKind, evidenceRef, ruleId, severity, status,
            statusActorId, statusAt, title, detail, occurredAt, escalationLevel,
            escalatedAt, escalatedTo, version
        )
    }
}
