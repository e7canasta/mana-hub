package com.hub.surveillance.domain.model

import com.hub.shared.domain.AggregateRoot
import com.hub.shared.domain.EpisodeId
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.BedId
import com.hub.surveillance.domain.event.EpisodeEvent
import java.time.Instant

@ConsistentCopyVisibility
data class Episode private constructor(
    override val id: EpisodeId,
    val residentId: ResidentId,
    val bedId: BedId?,
    val evidenceKind: String?,
    val evidenceRef: String?,
    val ruleId: String?,
    val severity: EpisodeSeverity,
    val status: EpisodeStatus,
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

    fun acknowledge(actorId: String): Episode {
        require(status == EpisodeStatus.PENDING) { "Episode is not pending" }
        val next = copy(
            status = EpisodeStatus.ACKNOWLEDGED, statusActorId = actorId, statusAt = Instant.now(),
        )
        next._domainEvents.add(
            EpisodeEvent.Acknowledged(episodeId = id, actorId = actorId)
        )
        return next
    }

    fun resolve(actorId: String): Episode {
        val next = copy(
            status = EpisodeStatus.RESOLVED, statusActorId = actorId, statusAt = Instant.now(),
        )
        next._domainEvents.add(
            EpisodeEvent.Resolved(episodeId = id, actorId = actorId)
        )
        return next
    }

    fun escalate(targetId: String): Episode {
        val next = copy(
            escalationLevel = escalationLevel + 1, escalatedAt = Instant.now(),
            escalatedTo = targetId
        )
        next._domainEvents.add(
            EpisodeEvent.Escalated(episodeId = id, targetId = targetId, newLevel = next.escalationLevel)
        )
        return next
    }

    fun complicated(newSeverity: EpisodeSeverity, targetId: String, detail: String? = null): Episode {
        val elevated = elevateSeverity(newSeverity, detail ?: "Complicated $severity → $newSeverity")
        return elevated.escalate(targetId)
    }

    fun elevateSeverity(newSeverity: EpisodeSeverity, newDetail: String?): Episode {
        if (!newSeverity.isMoreSevereThan(this.severity)) return this
        return copy(
            severity = newSeverity, detail = newDetail ?: detail
        )
    }

    companion object {
        fun create(
            residentId: ResidentId, bedId: BedId?, severity: EpisodeSeverity, title: String?,
            detail: String?, occurredAt: Instant, evidenceKind: String? = null, evidenceRef: String? = null,
            id: EpisodeId? = null,
        ): Episode {
            val episode = Episode(
                id = id ?: EpisodeId.random(), residentId = residentId, bedId = bedId, evidenceKind = evidenceKind,
                evidenceRef = evidenceRef, ruleId = null, severity = severity, status = EpisodeStatus.PENDING,
                statusActorId = null, statusAt = null, title = title, detail = detail,
                occurredAt = occurredAt, escalationLevel = 0, escalatedAt = null, escalatedTo = null, version = 0
            )
            episode._domainEvents.add(
                EpisodeEvent.Created(
                    episodeId = episode.id, residentId = residentId,
                    bedId = bedId, severity = severity, title = title,
                )
            )
            return episode
        }

        /**
         * Factory richer: crea un episodio desde una señal de monitoreo.
         *
         * Vernon diría: "El aggregate debe saber cómo nace desde el contexto
         * que lo dispara, no delegar esa lógica al servicio de aplicación".
         *
         * Esta factory encapsula la traducción señal → episodio, que antes
         * vivía en IntegrationService como lógica procedimental.
         */
        fun fromSignal(
            residentId: ResidentId,
            bedId: BedId,
            signalType: String,
            severity: EpisodeSeverity,
            title: String,
            detail: String?,
            occurredAt: Instant,
            evidenceKind: String? = null,
            evidenceRef: String? = null,
        ): Episode = create(
            residentId = residentId,
            bedId = bedId,
            severity = severity,
            title = title,
            detail = detail ?: "Signal: $signalType",
            occurredAt = occurredAt,
            evidenceKind = evidenceKind,
            evidenceRef = evidenceRef,
        )

        fun reconstitute(
            id: EpisodeId, residentId: ResidentId, bedId: BedId?, evidenceKind: String?,
            evidenceRef: String?, ruleId: String?, severity: EpisodeSeverity, status: EpisodeStatus,
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
