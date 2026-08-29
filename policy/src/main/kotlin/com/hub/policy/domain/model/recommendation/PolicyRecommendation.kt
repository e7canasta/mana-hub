package com.hub.policy.domain.model.recommendation

import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.recommendation.Recommendation
import com.hub.shared.domain.recommendation.RecommendationId
import com.hub.shared.domain.recommendation.RecommendationOrigin
import com.hub.shared.domain.recommendation.RecommendationState
import java.time.Instant

/**
 * Recomendación específica de política de alarma.
 *
 * Nace de un episodio (fuente) y se muestra en la pantalla de presets del residente (destino).
 * El director puede aprobar o descartar. Al aprobar, un proceso aplica el [presetPatch] al preset vigente.
 */
data class PolicyRecommendation(
    override val id: RecommendationId,
    val episodeId: String,
    override val residentId: ResidentId,
    override val title: String,
    override val description: String,
    val presetPatch: PresetPatch,
    override val origin: RecommendationOrigin,
    override val state: RecommendationState,
    override val createdAt: Instant,
    override val resolvedAt: Instant? = null,
    val appliedAt: Instant? = null,
) : Recommendation {

    fun approve(): PolicyRecommendation {
        require(state == RecommendationState.PENDING) { "Solo se pueden aprobar recomendaciones pendientes" }
        return copy(
            state = RecommendationState.APPROVED,
            resolvedAt = Instant.now(),
        )
    }

    fun apply(): PolicyRecommendation {
        require(state == RecommendationState.APPROVED) { "Solo se pueden aplicar recomendaciones aprobadas" }
        return copy(
            state = RecommendationState.APPLIED,
            appliedAt = Instant.now(),
        )
    }

    fun discard(): PolicyRecommendation {
        require(state == RecommendationState.PENDING) { "Solo se pueden descartar recomendaciones pendientes" }
        return copy(
            state = RecommendationState.DISCARDED,
            resolvedAt = Instant.now(),
        )
    }
}
