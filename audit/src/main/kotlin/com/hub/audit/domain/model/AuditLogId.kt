package com.hub.audit.domain.model

import java.util.UUID

@JvmInline
value class AuditLogId(val value: String) {
    companion object {
        fun from(value: String): AuditLogId = AuditLogId(value)
        fun random(): AuditLogId = AuditLogId(UUID.randomUUID().toString())
    }
}
