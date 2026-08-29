package com.hub.history.application.service

import com.hub.history.domain.model.timeline.EpisodeTimelineEvent
import com.hub.history.domain.model.timeline.EpisodeTimelineEventId
import com.hub.history.domain.model.timeline.EpisodeTimelineRepository
import com.hub.history.domain.model.timeline.EventType
import com.hub.shared.domain.ResidentId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Construye el EpisodeTimeline cuando un episodio se cierra.
 *
 * Consolida scene events, sentinel events y alarm notifications
 * en una sola línea de tiempo curada para el director médico.
 */
@Service
class EpisodeTimelineBuilder(
    private val timelineRepository: EpisodeTimelineRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun onSentinelEvent(
        eventId: String,
        eventType: String,
        episodeId: String,
        residentId: String,
        bedId: String?,
        severity: String?,
        fromState: String?,
        toState: String?,
        description: String?,
        occurredAt: Instant,
    ) {
        val mappedType = mapSentinelEventType(eventType) ?: return

        val event = EpisodeTimelineEvent(
            id = EpisodeTimelineEventId.from(eventId),
            episodeId = episodeId,
            residentId = ResidentId(residentId),
            at = occurredAt,
            type = mappedType,
            fromState = fromState,
            toState = toState,
            description = description ?: "$eventType for episode $episodeId",
        )

        try {
            timelineRepository.save(event)
            log.info("Timeline event {} for episode {} at {}", mappedType, episodeId, occurredAt)
        } catch (e: Exception) {
            if (e.message?.contains("duplicate", true) == true || e.message?.contains("Unique", true) == true) {
                log.debug("Duplicate timeline event ignored: {}", event.id)
            } else {
                log.error("Failed to save timeline event: {}", e.message)
            }
        }
    }

    @Transactional
    fun onSceneEvent(
        eventId: String,
        episodeId: String?,
        residentId: String?,
        bedId: String?,
        fromState: String?,
        toState: String?,
        triggerType: String?,
        occurredAt: Instant,
    ) {
        // Solo guardamos scene events si hay un episodio activo
        if (episodeId == null || residentId == null) return

        val event = EpisodeTimelineEvent(
            id = EpisodeTimelineEventId.from(eventId),
            episodeId = episodeId,
            residentId = ResidentId(residentId),
            at = occurredAt,
            type = EventType.UMBRELLA,
            fromState = fromState,
            toState = toState,
            description = "Movimiento detectado: $fromState → $toState",
        )

        try {
            timelineRepository.save(event)
            log.debug("Timeline UMBRELLA for episode {} at {}", episodeId, occurredAt)
        } catch (e: Exception) {
            if (e.message?.contains("duplicate", true) == true) {
                log.debug("Duplicate scene timeline event ignored")
            } else {
                log.error("Failed to save scene timeline event: {}", e.message)
            }
        }
    }

    @Transactional
    fun onAlarmEvent(
        eventId: String,
        episodeId: String?,
        residentId: String?,
        channel: String?,
        severity: String?,
        eventType: String,
        occurredAt: Instant,
    ) {
        if (episodeId == null || residentId == null) return

        val mappedType = when (eventType) {
            "Dispatch", "NoticeDispatched" -> EventType.NOTIFIED
            "Resolve", "NoticeResolved" -> EventType.RESPONDED
            else -> return
        }

        val event = EpisodeTimelineEvent(
            id = EpisodeTimelineEventId.from(eventId),
            episodeId = episodeId,
            residentId = ResidentId(residentId),
            at = occurredAt,
            type = mappedType,
            fromState = null,
            toState = null,
            description = when (mappedType) {
                EventType.NOTIFIED -> "Notificación $channel enviada (severidad: $severity)"
                EventType.RESPONDED -> "Staff respondió vía $channel"
                else -> "$eventType via $channel"
            },
        )

        try {
            timelineRepository.save(event)
            log.info("Timeline {} for episode {} via {}", mappedType, episodeId, channel)
        } catch (e: Exception) {
            if (e.message?.contains("duplicate", true) == true) {
                log.debug("Duplicate alarm timeline event ignored")
            } else {
                log.error("Failed to save alarm timeline event: {}", e.message)
            }
        }
    }

    private fun mapSentinelEventType(type: String): EventType? = when (type) {
        "EpisodeOpened", "EpisodeDeclared", "IncidentDeclared" -> EventType.OPENED
        "EpisodeClosed", "EpisodeResolved" -> EventType.CLOSED
        "AutoRecovery" -> EventType.RECOVERY
        "SeverityRamp", "EpisodeEscalated" -> EventType.ESCALATED
        else -> null
    }
}
