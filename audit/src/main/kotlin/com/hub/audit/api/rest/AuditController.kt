package com.hub.audit.api.rest

import com.hub.audit.domain.model.AuditLogEntry
import com.hub.audit.domain.repository.AuditLogRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/audit-log")
class AuditController(
    private val auditLogRepository: AuditLogRepository
) {

    @GetMapping
    fun listAuditLog(
        @RequestParam entityType: String?,
        @RequestParam entityId: String?,
        @RequestParam actorId: String?
    ): ResponseEntity<List<AuditLogEntry>> {
        val entries = when {
            entityType != null && entityId != null ->
                auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId)
            actorId != null ->
                auditLogRepository.findByActorId(actorId)
            else ->
                emptyList()
        }
        return ResponseEntity.ok(entries)
    }
}
