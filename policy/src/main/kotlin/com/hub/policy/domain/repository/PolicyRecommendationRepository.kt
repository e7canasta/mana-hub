package com.hub.policy.domain.repository

import com.hub.policy.domain.model.recommendation.PolicyRecommendation
import com.hub.shared.domain.ResidentId
import com.hub.policy.domain.model.recommendation.RecommendationId
import com.hub.policy.domain.model.recommendation.RecommendationState

interface PolicyRecommendationRepository {
    fun findById(id: RecommendationId): PolicyRecommendation?
    fun findByResidentIdAndState(residentId: ResidentId, state: RecommendationState): List<PolicyRecommendation>
    fun save(recommendation: PolicyRecommendation): PolicyRecommendation
}
