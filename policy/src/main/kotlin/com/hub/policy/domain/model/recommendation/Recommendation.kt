package com.hub.policy.domain.model.recommendation

import com.hub.shared.domain.ResidentId
import java.time.Instant

/**
 * Recomendación genérica — ciclo de vida compartido.
 *
 * Cada módulo implementa con su tipo específico:
 * - PolicyRecommendation (policy module)
 * - StaffRecommendation (population module, futuro)
 * - etc.
 */
interface Recommendation {
    val id: RecommendationId
    val residentId: ResidentId
    val title: String
    val description: String
    val origin: RecommendationOrigin
    val state: RecommendationState
    val createdAt: Instant
    val resolvedAt: Instant?
}
