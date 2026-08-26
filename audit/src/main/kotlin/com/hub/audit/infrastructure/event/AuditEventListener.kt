package com.hub.audit.infrastructure.event

import com.hub.audit.domain.service.AuditService
import com.hub.shared.domain.DomainEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class AuditEventListener(
    private val auditService: AuditService
) {
    @EventListener
    fun handleDomainEvent(event: DomainEvent) {
        auditService.recordEvent(event, null)
    }
}
