package com.hub.policy.domain.model.recommendation

import java.util.UUID

@JvmInline
value class RecommendationId(val value: UUID = UUID.randomUUID()) {
    companion object {
        fun from(raw: String) = RecommendationId(UUID.fromString(raw))
    }
}
