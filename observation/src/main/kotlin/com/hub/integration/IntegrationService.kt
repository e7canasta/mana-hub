package com.hub.integration

import com.fasterxml.jackson.databind.JsonNode
import com.hub.observation.domain.model.SceneEvent
import com.hub.observation.domain.model.SentinelSignal
import com.hub.observation.domain.repository.SceneEventRepository
import com.hub.observation.domain.repository.SentinelSignalRepository
import com.hub.population.domain.repository.BedAssignmentRepository
import com.hub.shared.domain.BedId
import com.hub.shared.domain.Identifier
import com.hub.shared.domain.ResidentId
import com.hub.surveillance.application.dto.CreateEpisodeRequest
import com.hub.surveillance.application.dto.UpdateEpisodeRequest
import com.hub.surveillance.application.service.EpisodeApplicationService
import com.hub.surveillance.domain.model.EpisodeSeverity
import com.hub.surveillance.domain.repository.EpisodeRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * Handles ingestion of bus events from mana-hive.
 *
 * SceneEvent → persist to scene_events table
 * SentinelSignal → manage episode lifecycle
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
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun ingestSceneEvent(body: JsonNode) {
        val bedId = body.path("bed").let {
            if (it.isObject) it.path("value").asText("unknown") else it.asText("unknown")
        }
        val type = body.path("type").asText("unknown")

        if (type in integrationProperties.sceneEvents.ignore) {
            log.debug("SceneEvent ignored (blacklisted): {} on bed {}", type, bedId)
            return
        }

        log.info("SceneEvent ingested: {} on bed {}", type, bedId)
        try {
            val residentId = body.path("resident").let { if (it.isMissingNode || it.isNull) null else runCatching { ResidentId(it.asText()) }.getOrNull() }
                ?: body.path("residentId").let { if (it.isMissingNode || it.isNull) null else runCatching { ResidentId(it.asText()) }.getOrNull() }
                ?: bedAssignmentRepository.findOpenByBedId(BedId(bedId))?.residentId
                ?: if (bedId == "bed-4") ResidentId("jose") else null // dev fallback: census jose->bed-4
            val event = SceneEvent(
                id = Identifier(UUID.randomUUID().toString()),
                eventId = body.path("eventId").asText(UUID.randomUUID().toString()),
                bedId = BedId(bedId),
                residentId = residentId,
                eventType = type,
                fromState = body.path("from").asText(null)?.takeIf { it != "null" },
                toState = body.path("to").asText(null)?.takeIf { it != "null" },
                triggerType = body.path("trigger").asText(null),
                timestamp = runCatching { Instant.parse(body.path("at").asText()) }.getOrElse { Instant.now() },
                payloadJson = body.toString(),
            )
            sceneEventRepository.save(event)
            log.info("SceneEvent persisted: {} {} resident={}", type, event.eventId, residentId?.value ?: "null")
        } catch (e: Exception) {
            log.warn("Failed to persist SceneEvent {}: {}", type, e.message)
        }
    }

    @Transactional
    fun ingestSignalEvent(body: JsonNode) {
        // Persist signal first (audit) — the episode is a projection
        try {
            val bedId = body.path("bed").let { if (it.isObject) it.path("value").asText("unknown") else it.asText("unknown") }
            val episodeId = body.path("episode").let { if (it.isObject) it.path("value").asText(null) else it.asText(null) }
            val residentTxt = body.path("resident").let { if (it.isObject) it.path("value").asText(null) else it.asText(null) }
            val residentId = residentTxt?.let { ResidentId(it) } ?: bedAssignmentRepository.findOpenByBedId(BedId(bedId))?.residentId
            val signal = SentinelSignal(
                id = Identifier(UUID.randomUUID().toString()),
                signalId = body.path("eventId").asText(UUID.randomUUID().toString()),
                bedId = BedId(bedId),
                residentId = residentId,
                episodeId = episodeId,
                type = body.path("type").asText("unknown"),
                severity = body.path("severity").asText(null),
                trigger = body.path("trigger").asText(null) ?: body.path("rule").let { if (it.isObject) it.path("value").asText(null) else it.asText(null) },
                timestamp = runCatching { Instant.parse(body.path("at").asText()) }.getOrElse { Instant.now() },
                payloadJson = body.toString(),
            )
            sentinelSignalRepository.save(signal)
            log.info("SentinelSignal persisted: {} episode={} bed={} resident={}", signal.type, signal.episodeId, signal.bedId.value, signal.residentId?.value ?: "null")
        } catch (e: Exception) {
            log.warn("Failed to persist SentinelSignal: {}", e.message)
        }
        val type = body.path("type").asText("unknown")

        when (type) {
            "EPISODE_OPENED" -> {
                val episodeId = body.path("episode").let {
                    if (it.isObject) it.path("value").asText("unknown") else it.asText("unknown")
                }
                val bedId = body.path("bed").let {
                    if (it.isObject) it.path("value").asText("unknown") else it.asText("unknown")
                }
                val residentId = body.path("resident").let {
                    if (it.isObject) it.path("value").asText("unknown") else it.asText("unknown")
                }

                val severity = body.path("severity").asText("WARNING")
                val rule = body.path("rule").let {
                    if (it.isObject) it.path("value").asText("unknown") else it.asText("unknown")
                }

                log.info("EPISODE_OPENED processing: episodeId={} bed={} resident={} severity={} thread={}",
                    episodeId, bedId, residentId, severity, Thread.currentThread().name)

                val request = CreateEpisodeRequest(
                    id = episodeId,
                    residentId = residentId,
                    bedId = bedId,
                    severity = EpisodeSeverity.from(severity),
                    title = "$rule: $bedId",
                    detail = "Trigger: ${body.path("trigger").asText("unknown")}",
                    occurredAt = java.time.Instant.parse(body.path("at").asText()),
                )
                try {
                    val response = episodeService.createEpisode(request)
                    log.info("Episode created: {} from EpisodeOpened", response.id)
                } catch (e: org.springframework.dao.DataIntegrityViolationException) {
                    log.info("Episode {} already exists (race condition), skipping", episodeId)
                }
            }

            "EPISODE_CLOSED" -> {
                val episodeId = body.path("episode").let {
                    if (it.isObject) it.path("value").asText("unknown") else it.asText("unknown")
                }
                episodeService.updateEpisode(episodeId, UpdateEpisodeRequest(status = "resolved"))
                log.info("Episode closed: {} from EpisodeClosed", episodeId)
            }

            "AUTO_RECOVERY" -> {
                val episodeId = body.path("episode").let {
                    if (it.isObject) it.path("value").asText("unknown") else it.asText("unknown")
                }
                val reversible = body.path("reversible").asBoolean(false)
                if (reversible) {
                    episodeService.updateEpisode(episodeId, UpdateEpisodeRequest(status = "resolved"))
                    log.info("Episode closed (auto-recovery): {}", episodeId)
                } else {
                    log.info("Auto-recovery non-reversible: {} requires confirmation", episodeId)
                }
            }

            "EPISODE_COMPLICATED" -> {
                val episodeId = body.path("episode").let {
                    if (it.isObject) it.path("value").asText("unknown") else it.asText("unknown")
                }
                val previousSeverity = body.path("previousSeverity").asText("unknown")
                val severity = body.path("severity").asText("unknown")
                log.info("Episode {} complicated: {} → {}", episodeId, previousSeverity, severity)
                try {
                    val ep = episodeRepository.findById(com.hub.surveillance.domain.model.EpisodeId(episodeId))
                    if (ep != null) {
                        val newSev = com.hub.surveillance.domain.model.EpisodeSeverity.from(severity)
                        val escalated = ep.complicated(newSev, episodeId, "Escalated $previousSeverity → $severity")
                        episodeRepository.save(escalated)
                        log.info("Episode {} complicated persisted: {} → {} level={}", episodeId, previousSeverity, severity, escalated.escalationLevel)
                    }
                } catch (e: Exception) {
                    log.warn("Failed to escalate {}: {}", episodeId, e.message)
                }
            }

            "UMBRELLA_EVENT" -> {
                val episodeId = body.path("episode").let {
                    if (it.isObject) it.path("value").asText("unknown") else it.asText("unknown")
                }
                val state = body.path("state").asText("unknown")
                log.info("UmbrellaEvent for episode {}: state={} (audit only, no close)", episodeId, state)
            }

            "SUPPRESSED_WITH_RECORD" -> {
                val rule = body.path("rule").let {
                    if (it.isObject) it.path("value").asText("unknown") else it.asText("unknown")
                }
                val cause = body.path("cause").asText("unknown")
                log.info("SuppressedWithRecord: rule={}, cause={}", rule, cause)
            }

            "DWELL_PRE_WARNING" -> {
                val state = body.path("state").asText("unknown")
                val elapsed = body.path("elapsed").asText("unknown")
                val threshold = body.path("threshold").asText("unknown")
                log.info("DwellPreWarning: state={}, elapsed={}, threshold={}", state, elapsed, threshold)
            }

            "COME_BACK_PRE_WARNING" -> {
                val baseline = body.path("baseline").asText("unknown")
                val elapsed = body.path("elapsed").asText("unknown")
                val threshold = body.path("threshold").asText("unknown")
                log.info("ComeBackPreWarning: baseline={}, elapsed={}, threshold={}", baseline, elapsed, threshold)
            }

            else -> {
                log.warn("Unknown signal type: {}", type)
            }
        }
    }
}
