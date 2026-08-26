package com.hub.audit.domain.repository

import com.hub.audit.domain.model.AuditLogEntry
import com.hub.audit.domain.model.AuditLogId

interface AuditLogRepository {
    fun save(entry: AuditLogEntry): AuditLogEntry
    fun findById(id: AuditLogId): AuditLogEntry?
    fun findByEntityTypeAndEntityId(entityType: String, entityId: String): List<AuditLogEntry>
    fun findByActorId(actorId: String): List<AuditLogEntry>
}
