package com.hub.bridge

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hub.observation.application.dto.IngestEventRequest
import com.hub.observation.application.dto.IngestSceneEventRequest
import com.hub.observation.application.service.EventIngestionService
import com.hub.shared.domain.BedId
import com.hub.shared.domain.ResidentId
import com.hub.surveillance.application.dto.CreateEpisodeRequest
import com.hub.surveillance.application.dto.UpdateEpisodeRequest
import com.hub.surveillance.application.service.EpisodeApplicationService
import com.hub.surveillance.domain.model.Episode
import com.hub.surveillance.domain.model.EpisodeId
import com.hub.surveillance.domain.repository.EpisodeRepository
import com.manahive.contracts.EventEnvelope
import com.manahive.messaging.NatsObjectMapper
import com.manahive.messaging.Subjects
import com.manahive.messaging.BusEvents
import io.nats.client.JetStreamSubscription
import io.nats.client.PushSubscribeOptions
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Bridge hive → hub : consume eventos de motores y persiste idempotente en SOR.
 *
 * JetStream durable pull con ack explícito (vs core NATS at-most-once).
 * Reutiliza cabeceras compartidas: EventEnvelope + Subjects.
 *
 * Durables: hub-perception, hub-scene, hub-sentinel — si hub reinicia, JetStream
 * retiene 7 días y reentrega. Mensajes con mismo eventId se ignoran por UNIQUE.
 */
@Component
@ConditionalOnProperty(name = ["nats.enabled"], havingValue = "true")
class HiveEventConsumer(
    private val events: BusEvents,
    private val eventIngestionService: EventIngestionService,
    private val episodeService: EpisodeApplicationService,
    private val episodeRepository: EpisodeRepository,
    private val objectMapper: ObjectMapper = NatsObjectMapper.mapper
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val subscriptions = mutableListOf<JetStreamSubscription>()

    @PostConstruct
    fun init() {
        // Hive NightWatchApplication.kt:50 — re-suscribir en cada reconexión
        events.onConnected { subscribeInternal() }
        events.onLost { reason -> log.warn("Bus lost ({}), consumers paused until reconnect", reason) }
        // Si BusEvents ya tiene conexión (arranque tardío), suscribir ya
        if (events.connected) subscribeInternal()
    }

    @Synchronized
    private fun subscribeInternal() {
        // Limpiar suscripciones previas de conexión caída
        subscriptions.forEach { try { it.unsubscribe() } catch (_: Exception) {} }
        subscriptions.clear()
        try {
            val js = events.connection!!.jetStream()
            subscriptions += js.subscribe(Subjects.PERCEPTION_WILDCARD, PushSubscribeOptions.builder().durable("hub-perception").deliverGroup("hub").build()).also { sub -> handleJetStream(sub, ::handlePerception) }
            subscriptions += js.subscribe(Subjects.SCENE_WILDCARD, PushSubscribeOptions.builder().durable("hub-scene").deliverGroup("hub").build()).also { sub -> handleJetStream(sub, ::handleScene) }
            subscriptions += js.subscribe(Subjects.SENTINEL_WILDCARD, PushSubscribeOptions.builder().durable("hub-sentinel").deliverGroup("hub").build()).also { sub -> handleJetStream(sub, ::handleSentinel) }
            log.info("HiveEventConsumer JetStream durables (re)subscribed: {}", subscriptions.size)
        } catch (e: Exception) {
            log.warn("HiveEventConsumer JetStream subscribe failed (NATS not ready): {}", e.message)
        }
    }

    @PreDestroy
    fun close() { subscriptions.forEach { try { it.unsubscribe() } catch (_: Exception) {} } }

    private fun handleJetStream(sub: JetStreamSubscription, handler: (EventEnvelope, String) -> Unit) {
        Thread {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val msg = sub.nextMessage(java.time.Duration.ofSeconds(1)) ?: continue
                    val envelope = objectMapper.readValue<EventEnvelope>(String(msg.data, Charsets.UTF_8))
                    try {
                        handler(envelope, envelope.payloadJson)
                        msg.ack()
                    } catch (e: Exception) {
                        log.error("Handler failed for {}: {}", envelope.eventId, e.message)
                        msg.nak()
                    }
                } catch (_: InterruptedException) { break }
                catch (_: Exception) { /* timeout */ }
            }
        }.apply { isDaemon = true; start() }
    }

    private fun handlePerception(envelope: EventEnvelope, payload: String) {
        val node = objectMapper.readTree(payload)
        val req = IngestEventRequest(
            sourceEventId = envelope.eventId,
            monitorKey = node.get("monitorKey")?.asText() ?: node.get("bedId")?.asText() ?: "unknown",
            bedId = node.get("bedId")?.asText(),
            residentId = node.get("residentId")?.asText(),
            kind = node.get("kind")?.asText() ?: "LOCATION",
            state = node.get("state")?.asText(),
            roomState = node.get("roomState")?.asText(),
            sleeping = node.get("sleeping")?.asBoolean(),
            occurredAt = envelope.occurredAt,
            payloadJson = payload
        )
        try { eventIngestionService.ingestEvent(req) } catch (e: Exception) {
            if (e.message?.contains("duplicate", true) == true) log.debug("Duplicate perception {} ignored", envelope.eventId) else throw e
        }
    }

    private fun handleScene(envelope: EventEnvelope, payload: String) {
        val node = objectMapper.readTree(payload)
        val req = IngestSceneEventRequest(
            sourceEventId = envelope.eventId,
            eventId = envelope.eventId,
            bedId = node.get("bedId")?.asText() ?: node.get("bed")?.asText() ?: "unknown",
            residentId = node.get("residentId")?.asText(),
            eventType = node.get("eventType")?.asText() ?: node.get("type")?.asText() ?: "TRANSITION",
            fromState = node.get("fromState")?.asText(),
            toState = node.get("toState")?.asText(),
            triggerType = node.get("triggerType")?.asText(),
            timestamp = envelope.occurredAt,
            payloadJson = payload
        )
        try { eventIngestionService.ingestSceneEvent(req) } catch (e: Exception) {
            if (e.message?.contains("duplicate", true) == true) log.debug("Duplicate scene {} ignored", envelope.eventId) else throw e
        }
    }

    private fun handleSentinel(envelope: EventEnvelope, payload: String) {
        val node = objectMapper.readTree(payload)
        val type = envelope.type
        val residentId = node.get("residentId")?.asText() ?: return
        val bedId = node.get("bedId")?.asText()
        val severityStr = node.get("severity")?.asText() ?: "WARNING"
        val severity = try { com.hub.surveillance.domain.model.EpisodeSeverity.valueOf(severityStr.uppercase()) } catch (_: Exception) { com.hub.surveillance.domain.model.EpisodeSeverity.WARNING }
        val title = node.get("title")?.asText() ?: "Sentinel: $type"
        when (type) {
            "EpisodeOpened", "EpisodeDeclared", "IncidentDeclared" -> {
                val open = episodeRepository.findOpenByResidentId(ResidentId(residentId))
                if (open != null) {
                    if (severity.isMoreSevereThan(open.severity)) {
                        val elevated = open.elevateSeverity(severity, payload)
                        episodeRepository.save(elevated)
                        log.info("Elevated episode {} for {}: {} -> {}", open.id.value, residentId, open.severity, severity)
                    } else {
                        log.debug("Episode {} already open for {}, ignoring lower severity {}", open.id.value, residentId, severity)
                    }
                } else {
                    val episode = Episode.reconstitute(
                        id = EpisodeId(envelope.eventId),
                        residentId = ResidentId(residentId),
                        bedId = bedId?.let { BedId(it) },
                        evidenceKind = null,
                        evidenceRef = null,
                        ruleId = node.get("ruleId")?.asText(),
                        severity = severity,
                        status = "pending",
                        statusActorId = null,
                        statusAt = null,
                        title = title,
                        detail = payload,
                        occurredAt = envelope.occurredAt,
                        escalationLevel = 0,
                        escalatedAt = null,
                        escalatedTo = null,
                        version = 0
                    )
                    try { episodeRepository.save(episode); log.info("Created episode {} for {} severity {}", episode.id.value, residentId, severity) }
                    catch (e: Exception) { if (e.message?.contains("duplicate", true) == true) log.debug("Duplicate episode {} ignored", envelope.eventId) else throw e }
                }
            }
            "EpisodeClosed", "EpisodeResolved" -> {
                val episodeId = node.get("episodeId")?.asText() ?: node.get("id")?.asText() ?: return
                try { episodeService.updateEpisode(episodeId, UpdateEpisodeRequest(status = "resolved")) } catch (_: Exception) {}
            }
        }
    }
}
