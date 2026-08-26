package com.hub.observation.domain.model

import com.hub.shared.domain.Identifier
import com.hub.population.domain.model.ResidentId
import com.hub.residence.domain.model.BedId
import java.time.Instant

data class NotificationEvent(
    val id: Identifier,
    val category: String,
    val bedId: BedId?,
    val residentId: ResidentId?,
    val eventType: String,
    val timestamp: Instant,
    val ruleId: String?,
    val riskLevel: String?,
    val payloadJson: String,
    val receivedAt: Instant
) {
    companion object {
        fun create(
            category: String,
            bedId: String?,
            residentId: String?,
            eventType: String,
            timestamp: Instant,
            ruleId: String? = null,
            riskLevel: String? = null,
            payloadJson: String = "{}"
        ): NotificationEvent = NotificationEvent(
            id = Identifier.random(),
            category = category,
            bedId = bedId?.let { BedId(it) },
            residentId = residentId?.let { ResidentId(it) },
            eventType = eventType,
            timestamp = timestamp,
            ruleId = ruleId,
            riskLevel = riskLevel,
            payloadJson = payloadJson,
            receivedAt = Instant.now()
        )
    }
}
