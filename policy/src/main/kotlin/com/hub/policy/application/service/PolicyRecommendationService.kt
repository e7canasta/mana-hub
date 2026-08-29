package com.hub.policy.application.service

import com.hub.policy.domain.model.recommendation.PolicyRecommendation
import com.hub.policy.domain.repository.PolicyRecommendationRepository
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.recommendation.RecommendationId
import com.hub.shared.domain.recommendation.RecommendationState
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PolicyRecommendationService(
    private val repository: PolicyRecommendationRepository,
) {

    @Transactional(readOnly = true)
    fun getPendingByResident(residentId: String): List<PolicyRecommendation> {
        return repository.findByResidentIdAndState(ResidentId(residentId), RecommendationState.PENDING)
    }

    @Transactional
    fun approve(recommendationId: String): PolicyRecommendation {
        val rec = repository.findById(RecommendationId.from(recommendationId))
            ?: throw IllegalArgumentException("Recommendation not found: $recommendationId")
        return repository.save(rec.approve())
    }

    @Transactional
    fun discard(recommendationId: String): PolicyRecommendation {
        val rec = repository.findById(RecommendationId.from(recommendationId))
            ?: throw IllegalArgumentException("Recommendation not found: $recommendationId")
        return repository.save(rec.discard())
    }
}
