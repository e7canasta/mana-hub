package com.hub.bridge

import com.hub.policy.application.service.AlarmProfileChangedEvent
import com.hub.policy.domain.repository.AlarmProfileRepository
import com.hub.shared.domain.ResidentId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Escucha AlarmProfileChangedEvent (publicado por AlarmProfileApplicationService)
 * y encola en outbox para que HubPolicyOutboxRelay lo publique a NATS.
 *
 * Usa @TransactionalEventListener(phase = AFTER_COMMIT) para asegurar que el
 * outbox se escribe solo si la TX de alarm_profile_versions committea.
 * Reutiliza HubPolicyPublisher (que escribe outbox, no publica directo).
 */
@Component
class HubPolicyBridgeListener(
    private val alarmProfileRepository: AlarmProfileRepository,
    private val hubPolicyPublisher: HubPolicyPublisher
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onProfileChanged(event: AlarmProfileChangedEvent) {
        try {
            val version = alarmProfileRepository.findCurrentByResidentId(ResidentId(event.residentId))
                ?: alarmProfileRepository.findByResidentId(ResidentId(event.residentId)).firstOrNull()
            if (version == null) {
                log.warn("No profile found for resident {} after change, skipping bridge", event.residentId)
                return
            }
            hubPolicyPublisher.publishChange(
                version = version,
                eventId = event.eventId,
                occurredAt = event.occurredAt
            )
            log.info("Bridged AlarmProfileChanged {} → outbox {} for hive", event.residentId, event.eventId)
        } catch (e: Exception) {
            log.error("Failed to bridge policy change for {}: {}", event.residentId, e.message)
        }
    }
}
