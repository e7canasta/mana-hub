package com.hub.audit.domain.model

import java.time.Instant

data class AuditLogEntry(
    val id: AuditLogId,
    val actorId: String?,
    val action: String,
    val entityType: String,
    val entityId: String,
    val metadataJson: String = "{}",
    val createdAt: Instant = Instant.now()
) {
    companion object {
        fun create(
            actorId: String?,
            action: String,
            entityType: String,
            entityId: String,
            metadataJson: String = "{}"
        ): AuditLogEntry = AuditLogEntry(
            id = AuditLogId.random(),
            actorId = actorId,
            action = action,
            entityType = entityType,
            entityId = entityId,
            metadataJson = metadataJson
        )
    }
}
