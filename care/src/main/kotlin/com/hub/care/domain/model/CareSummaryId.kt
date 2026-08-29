package com.hub.care.domain.model

import java.util.UUID

@JvmInline
value class CareSummaryId(val value: String) {
    companion object {
        fun from(value: String): CareSummaryId = CareSummaryId(value)
        fun random(): CareSummaryId = CareSummaryId(UUID.randomUUID().toString())
    }
}
