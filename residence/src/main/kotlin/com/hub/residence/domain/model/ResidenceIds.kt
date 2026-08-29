package com.hub.residence.domain.model

import java.util.UUID

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
