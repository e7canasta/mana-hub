package com.hub.streams.domain.model

import java.util.UUID

@JvmInline
value class StreamId(val value: String) {
    companion object {
        fun from(value: String): StreamId = StreamId(value)
        fun random(): StreamId = StreamId(UUID.randomUUID().toString())
    }
}
