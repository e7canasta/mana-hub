package com.hub.policy.domain.repository

import com.hub.policy.domain.model.recommendation.PolicyRecommendation
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.recommendation.RecommendationId
import com.hub.shared.domain.recommendation.RecommendationState

interface PolicyRecommendationRepository {
    fun findById(id: RecommendationId): PolicyRecommendation?
    fun findByResidentIdAndState(residentId: ResidentId, state: RecommendationState): List<PolicyRecommendation>
    fun save(recommendation: PolicyRecommendation): PolicyRecommendation
}
