package com.hub.observation.infrastructure.adapter

import com.hub.shared.domain.port.*
import com.hub.observation.domain.model.*
import com.hub.observation.domain.repository.SceneEventRepository
import com.hub.observation.domain.repository.SentinelSignalRepository
import com.hub.shared.domain.BedId
import com.hub.shared.domain.Identifier
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SceneEventAdapter(private val repo: SceneEventRepository) : SceneEventPort {
    override fun save(event: SceneEventModel) {
        val log = LoggerFactory.getLogger(SceneEventAdapter::class.java)
        val domainEvent = SceneEvent(
            id = event.id,
            eventId = event.eventId,
            bedId = event.bedId,
            residentId = event.residentId,
            eventType = event.eventType?.let { value ->
                SceneEventType.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                    ?: run {
                        log.warn("Unknown SceneEventType: '{}', defaulting to STATE_CHANGED", value)
                        SceneEventType.STATE_CHANGED
                    }
            },
            fromState = event.fromState?.let { value ->
                SceneState.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                    ?: run {
                        log.warn("Unknown SceneState: '{}', defaulting to UNKNOWN", value)
                        SceneState.UNKNOWN
                    }
            },
            toState = event.toState?.let { value ->
                SceneState.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                    ?: run {
                        log.warn("Unknown SceneState: '{}', defaulting to UNKNOWN", value)
                        SceneState.UNKNOWN
                    }
            },
            triggerType = event.triggerType?.let { value ->
                TriggerType.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                    ?: run {
                        log.warn("Unknown TriggerType: '{}', defaulting to SCHEDULED", value)
                        TriggerType.SCHEDULED
                    }
            },
            timestamp = event.timestamp,
            payloadJson = event.payloadJson,
            twinSnapshotJson = event.twinSnapshotJson,
            stateSince = event.stateSince,
            sceneSince = event.sceneSince,
            signalLost = event.signalLost,
            monitorId = event.monitorId,
        )
        repo.save(domainEvent)
    }
}

@Component
class SentinelSignalAdapter(private val repo: SentinelSignalRepository) : SentinelSignalPort {
    override fun save(signal: SentinelSignalModel) {
        val domainSignal = SentinelSignal(
            id = signal.id,
            signalId = signal.signalId,
            bedId = signal.bedId,
            residentId = signal.residentId,
            episodeId = signal.episodeId,
            type = SentinelSignalType.from(signal.type),
            severity = signal.severity,
            trigger = signal.trigger,
            timestamp = signal.timestamp,
            payloadJson = signal.payloadJson,
            ruleId = signal.ruleId,
            field = signal.field,
            triggerOn = signal.triggerOn,
            cause = signal.cause,
            state = signal.state,
            baseline = signal.baseline,
            rulesFingerprint = signal.rulesFingerprint,
            gapDuration = signal.gapDuration,
            previousSeverity = signal.previousSeverity,
            originalSeverity = signal.originalSeverity,
            reversible = signal.reversible,
            requiresNvr = signal.requiresNvr,
            confirmationWindow = signal.confirmationWindow,
            requiresConfirmation = signal.requiresConfirmation,
            elapsed = signal.elapsed,
            threshold = signal.threshold,
        )
        repo.save(domainSignal)
    }
}
