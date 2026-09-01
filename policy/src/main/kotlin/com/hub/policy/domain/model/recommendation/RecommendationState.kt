package com.hub.policy.domain.model.recommendation

enum class RecommendationState {
    PENDING,    // esperando decisión
    APPROVED,   // aceptada → pendiente de aplicación
    APPLIED,    // parche aplicado al preset
    DISCARDED,  // descartada
}
