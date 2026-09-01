package com.hub.shared.domain

import java.util.UUID

/**
 * Tipos de identidad compartidos entre bounded contexts.
 * Cada uno es un value object tipado que evita confundir IDs de distintos conceptos.
 */

@JvmInline
value class FacilityId(val value: String) {
    companion object {
        fun from(value: String): FacilityId = FacilityId(value)
        fun random(): FacilityId = FacilityId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class ResidentId(val value: String) {
    companion object {
        fun from(value: String): ResidentId = ResidentId(value)
        fun random(): ResidentId = ResidentId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class BedId(val value: String) {
    companion object {
        fun from(value: String): BedId = BedId(value)
        fun random(): BedId = BedId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class UserId(val value: String) {
    companion object {
        fun from(value: String): UserId = UserId(value)
        fun random(): UserId = UserId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class StaffMemberId(val value: String) {
    companion object {
        fun from(value: String): StaffMemberId = StaffMemberId(value)
        fun random(): StaffMemberId = StaffMemberId(UUID.randomUUID().toString())
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
