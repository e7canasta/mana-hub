package com.hub.clients.audit

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class AuditLogEntry(
    val id: String,
    @JsonProperty("actorId") val actorId: String? = null,
    val action: String,
    @JsonProperty("entityType") val entityType: String,
    @JsonProperty("entityId") val entityId: String,
    @JsonProperty("metadataJson") val metadataJson: String = "{}",
    @JsonProperty("createdAt") val createdAt: Instant = Instant.now()
)
