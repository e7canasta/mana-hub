package com.hub.residence.domain.model

import java.util.UUID

@JvmInline
value class FacilityId(val value: String) {
    companion object {
        fun from(value: String): FacilityId = FacilityId(value)
        fun random(): FacilityId = FacilityId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class WingId(val value: String) {
    companion object {
        fun from(value: String): WingId = WingId(value)
        fun random(): WingId = WingId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class RoomId(val value: String) {
    companion object {
        fun from(value: String): RoomId = RoomId(value)
        fun random(): RoomId = RoomId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class BedId(val value: String) {
    companion object {
        fun from(value: String): BedId = BedId(value)
        fun random(): BedId = BedId(UUID.randomUUID().toString())
    }
}
