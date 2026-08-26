package com.hub.residence.domain.model

import com.hub.shared.domain.AggregateRoot
import java.time.Instant

data class PlanogramPlacement(
    val id: String,
    val wingId: WingId,
    val roomId: RoomId,
    val x: Double,
    val y: Double,
    val sortOrder: Int,
    val active: Boolean
)

data class RoomPrivacyRegion(
    val id: String,
    val roomId: RoomId,
    val x: Double,
    val y: Double,
    val w: Double,
    val h: Double,
    val active: Boolean
)
