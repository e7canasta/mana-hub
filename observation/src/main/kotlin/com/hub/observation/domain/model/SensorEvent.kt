package com.hub.observation.domain.model

import com.hub.shared.domain.AggregateRoot
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.BedId
import java.time.Instant

class SensorEvent private constructor(
    override val id: com.hub.shared.domain.Identifier,
    val sourceEventId: String,
    val monitorKey: String,
    val bedId: BedId?,
    val residentId: ResidentId?,
    val kind: SensorEventKind,
    val roomState: String?,
    val substate: String?,
    val zone: String?,
    val state: String?,
    val sleeping: Boolean?,
    val occurredAt: Instant,
    val receivedAt: Instant,
    val payloadJson: String,
    override var version: Long
) : AggregateRoot<com.hub.shared.domain.Identifier>() {

    companion object {
        fun create(
            sourceEventId: String, monitorKey: String, bedId: BedId?, residentId: ResidentId?,
            kind: SensorEventKind, roomState: String?, state: String?, sleeping: Boolean?, occurredAt: Instant
        ): SensorEvent = SensorEvent(
            id = com.hub.shared.domain.Identifier.random(), sourceEventId = sourceEventId,
            monitorKey = monitorKey, bedId = bedId, residentId = residentId, kind = kind,
            roomState = roomState, substate = null, zone = null, state = state, sleeping = sleeping,
            occurredAt = occurredAt, receivedAt = Instant.now(), payloadJson = "{}", version = 0
        )
    }
}
