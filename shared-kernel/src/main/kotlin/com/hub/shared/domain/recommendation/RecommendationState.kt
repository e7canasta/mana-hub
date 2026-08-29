package com.hub.shared.domain.recommendation

enum class RecommendationState {
    PENDING,    // esperando decisión
    APPROVED,   // aceptada → pendiente de aplicación
    APPLIED,    // parche aplicado al preset
    DISCARDED,  // descartada
}
