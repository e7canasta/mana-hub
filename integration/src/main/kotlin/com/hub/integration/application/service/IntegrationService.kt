package com.hub.integration.application.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.manahive.contracts.scene.SceneEvent as HiveSceneEvent
import com.hub.integration.IntegrationProperties
import com.hub.observation.domain.model.SceneEvent
import com.hub.observation.domain.model.SceneEventType
import com.hub.observation.domain.model.SceneState
import com.hub.observation.domain.model.SignalEnvelope
import com.hub.observation.domain.model.SignalType
import com.hub.observation.domain.model.SentinelSignal
import com.hub.observation.domain.model.SentinelSignalType
import com.hub.observation.domain.model.TriggerType
import com.hub.observation.domain.repository.SceneEventRepository
import com.hub.observation.domain.repository.SentinelSignalRepository
import com.hub.population.domain.repository.BedAssignmentRepository
import com.hub.shared.domain.BedId
import com.hub.shared.domain.Identifier
import com.hub.shared.domain.ResidentId
import com.hub.surveillance.application.dto.CreateEpisodeRequest
import com.hub.surveillance.application.dto.UpdateEpisodeRequest
import com.hub.surveillance.application.service.EpisodeApplicationService
import com.hub.surveillance.domain.model.EpisodeId
import com.hub.surveillance.domain.model.EpisodeSeverity
import com.hub.surveillance.domain.repository.EpisodeRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * Handles ingestion of bus events from mana-hive.
 *
 * SceneEvent   → persist to scene_events table (filtered by config)
 * SentinelSignal → persist audit + manage episode lifecycle
 */
@Service
@EnableConfigurationProperties(IntegrationProperties::class)
class IntegrationService(
    private val episodeService: EpisodeApplicationService,
    private val episodeRepository: EpisodeRepository,
    private val sceneEventRepository: SceneEventRepository,
    private val sentinelSignalRepository: SentinelSignalRepository,
    private val bedAssignmentRepository: BedAssignmentRepository,
    private val integrationProperties: IntegrationProperties,
    private val objectMapper: ObjectMapper,
) {

    // ── Scene Events ────────────────────────────────────────────────

    @Transactional
    fun ingestSceneEvent(body: JsonNode) {
        val payload = ScenePayload.from(body)

        if (payload.type in integrationProperties.sceneEvents.ignore) {
            log.debug("SceneEvent ignored (blacklisted): {} on bed {}", payload.type, payload.bedId)
            return
        }

        try {
            val envelope = objectMapper.treeToValue(body, SignalEnvelope::class.java)
            val residentId = resolveResidentId(envelope.resident ?: envelope.residentId, payload.bedId)
            val twinNode = body.path("twinSnapshot")
            val twinJson = if (twinNode.isMissingNode || twinNode.isNull) "{}" else twinNode.toString()
            val stateSince = twinNode.path("stateSince").asText(null)?.let { runCatching { Instant.parse(it) }.getOrNull() }
            val sceneSince = twinNode.path("sceneSince").asText(null)?.let { runCatching { Instant.parse(it) }.getOrNull() }
            val signalLost = twinNode.path("signalLost").let { if (it.isMissingNode || it.isNull) null else it.asBoolean() }
            val monitorId = twinNode.path("monitor").let { n ->
                when {
                    n.isMissingNode || n.isNull -> null
                    n.isObject -> n.path("value").asText(null)
                    else -> n.asText(null)
                }
            } ?: twinNode.path("monitorId").asText(null)
            val event = SceneEvent(
                id = Identifier(UUID.randomUUID().toString()),
                eventId = payload.eventId,
                bedId = BedId(payload.bedId),
                residentId = residentId,
                eventType = runCatching { SceneEventType.from(payload.type) }.getOrNull(),
                fromState = hiveFromState(payload.type, body)?.let { SceneState.from(it) },
                toState = hiveToState(payload.type, body, twinNode)?.let { SceneState.from(it) },
                triggerType = body.path("trigger").asText(null)?.let { TriggerType.from(it) },
                timestamp = payload.timestamp,
                payloadJson = "{}", // no payload persistido — columnas + twinSnapshot son la fuente (V15/V17)
                twinSnapshotJson = twinJson,
                stateSince = stateSince,
                sceneSince = sceneSince,
                signalLost = signalLost,
                monitorId = monitorId,
            )
            sceneEventRepository.save(event)
            log.info("SceneEvent persisted: {} {} resident={} twinSnapshot={}", payload.type, payload.eventId, residentId?.value ?: "null", twinJson != "{}")
        } catch (e: Exception) {
            log.warn("Failed to persist SceneEvent {}: {}", payload.type, e.message)
        }
    }

    // ── Sentinel Signals ────────────────────────────────────────────

    @Transactional
    fun ingestSignalEvent(body: JsonNode) {
        val payload = SignalPayload.from(body)
        val envelope = objectMapper.treeToValue(body, SignalEnvelope::class.java)
        persistSignalAudit(payload, envelope)
        processSignalLifecycle(payload, envelope)
    }

    // ── Internal: Audit persistence ─────────────────────────────────

    private fun persistSignalAudit(payload: SignalPayload, envelope: SignalEnvelope) {
        try {
            val residentId = resolveResidentId(envelope.resident ?: envelope.residentId, payload.bedId)
            val signal = SentinelSignal(
                id = Identifier(UUID.randomUUID().toString()),
                signalId = payload.eventId,
                bedId = BedId(payload.bedId),
                residentId = residentId,
                episodeId = payload.episodeId,
                type = SentinelSignalType.from(payload.type?.name ?: envelope.type ?: "unknown"),
                severity = payload.severity ?: envelope.originalSeverity,
                trigger = payload.trigger ?: envelope.state ?: envelope.baseline,
                timestamp = payload.timestamp,
                payloadJson = "{}",
                ruleId = envelope.rule?.takeIf { it.isNotBlank() && it != "unknown" && it != "none" },
                field = envelope.field,
                triggerOn = envelope.triggerOn,
                cause = envelope.cause,
                state = envelope.state,
                baseline = envelope.baseline,
                rulesFingerprint = envelope.rulesFingerprint,
                gapDuration = envelope.gapDuration,
                previousSeverity = envelope.previousSeverity,
                originalSeverity = envelope.originalSeverity,
                reversible = envelope.reversible,
                requiresNvr = envelope.requiresNvr,
                confirmationWindow = envelope.confirmationWindow,
                requiresConfirmation = envelope.requiresConfirmation,
                elapsed = envelope.elapsed,
                threshold = envelope.threshold,
            )
            sentinelSignalRepository.save(signal)
            log.info("SentinelSignal audit: {} rule={} trigger={} cause={} episode={} bed={}", signal.type, envelope.rule ?: "-", signal.trigger ?: "-", envelope.cause ?: "-", payload.episodeId, payload.bedId)
        } catch (e: Exception) {
            log.warn("Failed to persist SentinelSignal audit: {}", e.message, e)
        }
    }

    // ── Internal: Episode lifecycle ─────────────────────────────────

    private fun processSignalLifecycle(payload: SignalPayload, envelope: SignalEnvelope) {
        when (payload.type) {
            SignalType.EPISODE_OPENED -> handleEpisodeOpened(payload, envelope)
            SignalType.EPISODE_CLOSED -> handleEpisodeClosed(payload)
            SignalType.AUTO_RECOVERY -> handleAutoRecovery(payload, envelope)
            SignalType.EPISODE_COMPLICATED -> handleEpisodeComplicated(payload, envelope)
            SignalType.UMBRELLA_EVENT -> handleUmbrellaEvent(payload, envelope)
            SignalType.SUPPRESSED_WITH_RECORD -> handleSuppressed(envelope)
            SignalType.DWELL_PRE_WARNING -> handlePreWarning("DwellPreWarning", envelope)
            SignalType.COME_BACK_PRE_WARNING -> handlePreWarning("ComeBackPreWarning", envelope)
            null -> log.warn("Unknown signal type: {}", envelope.type ?: "unknown")
        }
    }

    private fun handleEpisodeOpened(payload: SignalPayload, envelope: SignalEnvelope) {
        val residentId = envelope.resident ?: payload.bedId
        val ruleValue = envelope.rule ?: "unknown"
        val triggerValue = envelope.trigger ?: "unknown"

        log.info("EPISODE_OPENED: episode={} bed={} severity={}", payload.episodeId, payload.bedId, payload.severity)

        val request = CreateEpisodeRequest(
            id = payload.episodeId,
            residentId = residentId,
            bedId = payload.bedId,
            severity = EpisodeSeverity.from(payload.severity ?: "WARNING"),
            title = "$ruleValue: ${payload.bedId}",
            detail = "Trigger: $triggerValue",
            occurredAt = payload.timestamp,
        )
        try {
            val response = episodeService.createEpisode(request)
            log.info("Episode created: {} from EPISODE_OPENED", response.id)
        } catch (e: DataIntegrityViolationException) {
            log.info("Episode {} already exists (race condition), skipping", payload.episodeId)
        }
    }

    private fun handleEpisodeClosed(payload: SignalPayload) {
        episodeService.updateEpisode(payload.episodeId, UpdateEpisodeRequest(status = "resolved"))
        log.info("Episode closed: {} from EPISODE_CLOSED", payload.episodeId)
    }

    private fun handleAutoRecovery(payload: SignalPayload, envelope: SignalEnvelope) {
        val reversible = envelope.reversible ?: false
        if (reversible) {
            episodeService.updateEpisode(payload.episodeId, UpdateEpisodeRequest(status = "resolved"))
            log.info("Episode closed (auto-recovery): {}", payload.episodeId)
        } else {
            log.info("Auto-recovery non-reversible: {} requires confirmation", payload.episodeId)
        }
    }

    private fun handleEpisodeComplicated(payload: SignalPayload, envelope: SignalEnvelope) {
        val previousSeverity = envelope.previousSeverity ?: "unknown"
        log.info("Episode {} complicated: {} → {}", payload.episodeId, previousSeverity, payload.severity)
        try {
            val ep = episodeRepository.findById(EpisodeId(payload.episodeId))
            if (ep != null) {
                val newSev = EpisodeSeverity.from(payload.severity ?: "unknown")
                val escalated = ep.complicated(newSev, payload.episodeId, "Escalated $previousSeverity → ${payload.severity}")
                episodeRepository.save(escalated)
                log.info("Episode {} escalated: {} → {} level={}", payload.episodeId, previousSeverity, payload.severity, escalated.escalationLevel)
            }
        } catch (e: Exception) {
            log.warn("Failed to escalate {}: {}", payload.episodeId, e.message)
        }
    }

    private fun handleUmbrellaEvent(payload: SignalPayload, envelope: SignalEnvelope) {
        val state = envelope.state ?: "unknown"
        log.info("UmbrellaEvent for episode {}: state={} (audit only, no close)", payload.episodeId, state)
    }

    private fun handleSuppressed(envelope: SignalEnvelope) {
        log.info("SuppressedWithRecord: rule={}, cause={}", envelope.rule ?: "unknown", envelope.cause ?: "unknown")
    }

    private fun handlePreWarning(label: String, envelope: SignalEnvelope) {
        log.info("{}: state={}, elapsed={}, threshold={}", label, envelope.state ?: "unknown", envelope.elapsed ?: "unknown", envelope.threshold ?: "unknown")
    }

    // ── Internal: Shared helpers ────────────────────────────────────

    private fun resolveResidentId(resident: String?, bedId: String): ResidentId? {
        return resident?.let { runCatching { ResidentId(it) }.getOrNull() }
            ?: bedAssignmentRepository.findOpenByBedId(BedId(bedId))?.residentId
    }

    /**
     * Wire hive ([SceneEventSerializer]): TransitionDetected usa `from`/`to`
     * (simpleName de PersonState). NightOpened usa `initialState`, no `to`.
     * SceneStateChanged también trae `from`/`to` pero son flags de escena
     * (Present / NotPresent), no PersonState — se guardan igual en columnas.
     */
    private fun hiveFromState(type: String, body: JsonNode): String? {
        if (type == HiveSceneEvent.NightOpened::class.simpleName) return null
        return textOrNull(body, "from")
    }

    private fun hiveToState(type: String, body: JsonNode, twinNode: JsonNode): String? {
        val twinState = textOrNull(twinNode, "state")
        return when (type) {
            HiveSceneEvent.NightOpened::class.simpleName ->
                textOrNull(body, "initialState") ?: twinState
            HiveSceneEvent.TransitionDetected::class.simpleName ->
                textOrNull(body, "to") ?: twinState
            else -> textOrNull(body, "to")
        }
    }

    private fun textOrNull(node: JsonNode, field: String): String? =
        node.path(field).asText(null)?.takeIf { it.isNotBlank() && it != "null" }

    // ── Payload mappers ─────────────────────────────────────────────

    private data class ScenePayload(
        val bedId: String,
        val type: String,
        val eventId: String,
        val timestamp: Instant,
    ) {
        companion object {
            fun from(body: JsonNode) = ScenePayload(
                bedId = body.path("bed").let { if (it.isObject) it.path("value").asText("unknown") else it.asText("unknown") },
                type = body.path("type").asText("unknown"),
                eventId = body.path("eventId").asText(UUID.randomUUID().toString()),
                timestamp = runCatching { Instant.parse(body.path("at").asText()) }.getOrElse { Instant.now() },
            )
        }
    }

    private data class SignalPayload(
        val bedId: String,
        val type: SignalType?,
        val eventId: String,
        val episodeId: String,
        val severity: String?,
        val trigger: String?,
        val timestamp: Instant,
        val raw: JsonNode,
    ) {
        companion object {
            fun from(body: JsonNode): SignalPayload {
                val typeStr = body.path("type").asText("unknown")
                return SignalPayload(
                    bedId = body.path("bed").let { if (it.isObject) it.path("value").asText("unknown") else it.asText("unknown") },
                    type = SignalType.from(typeStr),
                    eventId = body.path("eventId").asText(UUID.randomUUID().toString()),
                    episodeId = body.path("episode").let { if (it.isObject) it.path("value").asText("unknown") else it.asText("unknown") },
                    severity = body.path("severity").asText(null),
                    trigger = body.path("trigger").asText(null) ?: body.path("rule").let { if (it.isObject) it.path("value").asText(null) else it.asText(null) },
                    timestamp = runCatching { Instant.parse(body.path("at").asText()) }.getOrElse { Instant.now() },
                    raw = body,
                )
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(IntegrationService::class.java)
    }
}
