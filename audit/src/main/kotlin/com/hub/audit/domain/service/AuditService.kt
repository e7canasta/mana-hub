package com.hub.audit.domain.service

import com.hub.audit.domain.model.AuditLogEntry
import com.hub.audit.domain.repository.AuditLogRepository
import com.hub.shared.domain.DomainEvent
import com.hub.shared.domain.DomainEventPublisher
import org.springframework.stereotype.Service

@Service
class AuditService(
    private val auditLogRepository: AuditLogRepository,
    private val eventPublisher: DomainEventPublisher
) {
    fun recordEvent(event: DomainEvent, actorId: String?) {
        val entry = AuditLogEntry.create(
            actorId = actorId,
            action = event.eventType,
            entityType = event::class.simpleName ?: "Unknown",
            entityId = event.eventId
        )
        auditLogRepository.save(entry)
    }

    fun recordAction(
        actorId: String?,
        action: String,
        entityType: String,
        entityId: String,
        metadataJson: String = "{}"
    ): AuditLogEntry {
        val entry = AuditLogEntry.create(
            actorId = actorId,
            action = action,
            entityType = entityType,
            entityId = entityId,
            metadataJson = metadataJson
        )
        return auditLogRepository.save(entry)
    }
}
