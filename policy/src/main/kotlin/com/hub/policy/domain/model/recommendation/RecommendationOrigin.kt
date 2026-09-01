package com.hub.policy.domain.model.recommendation

enum class RecommendationOrigin {
    MANUAL,      // alguien lo hizo a mano
    AUTOMATIC,   // el sistema lo generó
    COPILOT,     // el copiloto sugirió
}
