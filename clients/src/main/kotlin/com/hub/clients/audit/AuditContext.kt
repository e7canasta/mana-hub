package com.hub.clients.audit

import com.hub.clients.core.AuditDsl
import com.hub.clients.core.HttpApi

@AuditDsl
class AuditScope internal constructor(private val http: HttpApi) {

    fun byEntity(entityType: String, entityId: String): List<AuditLogEntry> =
        http.get("/api/v1/audit-log?entityType=$entityType&entityId=$entityId", Array<AuditLogEntry>::class.java).toList()

    fun byActor(actorId: String): List<AuditLogEntry> =
        http.get("/api/v1/audit-log?actorId=$actorId", Array<AuditLogEntry>::class.java).toList()
}
