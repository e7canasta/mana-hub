/**
 * SOURCE OF TRUTH KEYWORDS: SceneConfirmed, hub.scene.v1, SceneEvent, DomainEvent, persisted scene
 * WHAT: Represents a scene event after mana-hub has accepted and persisted it.
 * WHY: Consumers must distinguish Hub-confirmed scene facts from raw Hive facts.
 * WHERE: IntegrationService emits it after scene persistence and the NATS publisher routes it to hub.scene.
 */
package com.hub.shared.domain.event

import com.hub.shared.domain.BedId
import com.hub.shared.domain.DomainEvent
import com.hub.shared.domain.ResidentId
import java.time.Instant

data class SceneConfirmed(
    override val eventId: String,
    override val occurredAt: Instant,
    val bedId: BedId,
    val residentId: ResidentId?,
    val sceneType: String,
    val fromState: String?,
    val toState: String?,
    val triggerType: String?,
    val timestamp: Instant,
    val twinSnapshotJson: String,
) : DomainEvent {
    override val eventType: String = sceneType
}
