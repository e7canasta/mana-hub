package com.hub.bridge

import com.fasterxml.jackson.databind.ObjectMapper
import com.hub.policy.domain.model.AlarmProfileVersion
import com.manahive.contracts.EventEnvelope
import com.manahive.contracts.policy.HubMobilityAid
import com.manahive.contracts.policy.HubPolicyChange
import com.manahive.contracts.policy.HubPolicyMode
import com.manahive.contracts.policy.HubRiskLevel
import com.manahive.messaging.NatsObjectMapper
import com.manahive.messaging.Subjects
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * Bridge hub → hive : publica cambios de preset del director.
 *
 * Reutiliza cabeceras compartidas: EventEnvelope + Subjects (copiados de mana-hive/platform)
 * Payload es HubPolicyChange (JSON) — hive lo mapea a su AlarmProfile vía NatsObjectMapper.
 *
 * Se llama desde AlarmProfileApplicationService.updateResidentProfile() en la misma TX
 * (escribe outbox) + HubPolicyOutboxRelay publica async a JetStream.
 */
@Service
class HubPolicyPublisher(
    private val outboxRepository: HubPolicyOutboxRepository,
    private val objectMapper: ObjectMapper = NatsObjectMapper.mapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun publishChange(
        version: AlarmProfileVersion,
        eventId: String = UUID.randomUUID().toString(),
        occurredAt: Instant = Instant.now(),
        fingerprint: String = UUID.randomUUID().toString()
    ) {
        val payload = HubPolicyChange(
            residentId = version.residentId.value,
            at = occurredAt,
            riskLevel = HubRiskLevel.valueOf(version.riskLevel.name),
            templateId = version.templateId?.value,
            mobilityAid = version.mobilityAid?.let { HubMobilityAid.valueOf(it.name) },
            autopilot = version.autopilot,
            mode = version.mode?.let { HubPolicyMode.valueOf(it.name) },
            overridesJson = "{}",
            fingerprint = fingerprint
        )
        val envelope = EventEnvelope(
            eventId = eventId,
            type = "PolicyChangeDetected",
            version = 1,
            occurredAt = occurredAt,
            source = "mana-hub",
            payloadJson = objectMapper.writeValueAsString(payload)
        )
        // Outbox pattern: persiste en misma TX que alarm_profile_versions
        val outbox = HubPolicyOutboxEntity(
            id = envelope.eventId,
            aggregateId = version.residentId.value,
            type = envelope.type,
            payloadJson = objectMapper.writeValueAsString(envelope),
            occurredAt = envelope.occurredAt,
            published = false
        )
        outboxRepository.save(outbox)
        log.info("Enqueued PolicyChangeDetected outbox {} for resident {}", envelope.eventId, version.residentId.value)
    }

    /** Helper para publicar directo sin outbox (usado por tests/blueprints) */
    fun toEnvelopeJson(version: AlarmProfileVersion): String {
        val payload = HubPolicyChange(
            residentId = version.residentId.value,
            at = Instant.now(),
            riskLevel = HubRiskLevel.valueOf(version.riskLevel.name),
            templateId = version.templateId?.value,
            mobilityAid = version.mobilityAid?.let { HubMobilityAid.valueOf(it.name) },
            autopilot = version.autopilot,
            mode = version.mode?.let { HubPolicyMode.valueOf(it.name) },
            overridesJson = "{}",
            fingerprint = UUID.randomUUID().toString()
        )
        val envelope = EventEnvelope(
            eventId = UUID.randomUUID().toString(),
            type = "PolicyChangeDetected",
            version = 1,
            occurredAt = Instant.now(),
            source = "mana-hub",
            payloadJson = NatsObjectMapper.mapper.writeValueAsString(payload)
        )
        return NatsObjectMapper.mapper.writeValueAsString(envelope)
    }

    fun subjectForChange(): String = Subjects.policyChangeDetected()
    fun subjectForEffectiveRules(residentId: String): String = Subjects.effectiveRules(residentId)
}
