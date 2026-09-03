package com.hub.surveillance.application.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.hub.surveillance.domain.model.EpisodeSeverity
import com.hub.surveillance.domain.model.EpisodeStatus
import java.time.Instant

data class CreateEpisodeRequest(
    val id: String? = null,
    val residentId: String,
    val bedId: String? = null,
    val severity: EpisodeSeverity,
    val title: String? = null,
    val detail: String? = null,
    val occurredAt: Instant,
    val evidenceKind: String? = null,
    val evidenceRef: String? = null
)

data class EpisodeResponse(
    val id: String,
    val residentId: String,
    val bedId: String?,
    val severity: EpisodeSeverity,
    val status: EpisodeStatus,
    val title: String?,
    val detail: String?,
    val occurredAt: Instant,
    val escalationLevel: Int,
    @JsonProperty("isPending") val isPending: Boolean
)

data class AcknowledgeEpisodeRequest(
    val actorId: String
)

data class UpdateEpisodeRequest(
    val status: String? = null,
    val severity: String? = null,
    val title: String? = null,
    val detail: String? = null,
    val actorId: String? = null
)

data class CreateDeliveryRequest(
    val recipientKind: String,
    val recipientId: String,
    val channel: String
)

data class DeliveryResponse(
    val id: String,
    val episodeId: String,
    val recipientKind: String,
    val recipientId: String,
    val channel: String,
    val escalationLevel: Int
)

data class CreateDeliveryEventRequest(
    val kind: String,
    val reason: String? = null
)
