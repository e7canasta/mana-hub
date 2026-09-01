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
    private val log = LoggerFactory.getLogger(SceneEventAdapter::class.java)

    override fun save(event: SceneEventModel) {
        val domainEvent = SceneEvent(
            id = event.id,
            eventId = event.eventId,
            bedId = event.bedId,
            residentId = event.residentId,
            eventType = event.eventType?.let { parseEnum<SceneEventType>(it, SceneEventType.STATE_CHANGED) },
            fromState = event.fromState?.let { parseEnum<SceneState>(it, SceneState.UNKNOWN) },
            toState = event.toState?.let { parseEnum<SceneState>(it, SceneState.UNKNOWN) },
            triggerType = event.triggerType?.let { parseEnum<TriggerType>(it, TriggerType.SCHEDULED) },
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

    private inline fun <reified E : Enum<E>> parseEnum(value: String, default: E): E =
        enumValues<E>().firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: run { log.warn("Unknown {}: '{}', defaulting to {}", E::class.simpleName, value, default.name); default }
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
