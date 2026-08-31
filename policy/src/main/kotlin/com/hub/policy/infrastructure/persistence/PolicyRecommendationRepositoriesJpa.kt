package com.hub.policy.infrastructure.persistence

import com.hub.policy.domain.model.MobilityAid
import com.hub.policy.domain.model.PolicyMode
import com.hub.policy.domain.model.RiskLevel
import com.hub.policy.domain.model.TemplateId
import com.hub.policy.domain.model.recommendation.PolicyRecommendation
import com.hub.policy.domain.model.recommendation.PresetPatch
import com.hub.policy.domain.repository.PolicyRecommendationRepository
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.recommendation.RecommendationId
import com.hub.shared.domain.recommendation.RecommendationOrigin
import com.hub.shared.domain.recommendation.RecommendationState
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Entity
@Table(name = "policy_recommendations")
class PolicyRecommendationEntity(
    @Id var id: String = "",
    @Column(name = "episode_id") var episodeId: String = "",
    @Column(name = "resident_id") var residentId: String = "",
    @Column(name = "title") var title: String = "",
    @Column(name = "description") var description: String = "",
    @Column(name = "origin") var origin: String = "MANUAL",
    @Column(name = "state") var state: String = "PENDING",
    @Column(name = "patch_template_id") var patchTemplateId: String? = null,
    @Column(name = "patch_mode") var patchMode: String? = null,
    @Column(name = "patch_risk_level") var patchRiskLevel: String? = null,
    @Column(name = "patch_mobility_aid") var patchMobilityAid: String? = null,
    @Column(name = "patch_autopilot") var patchAutopilot: Boolean? = null,
    @Version var version: Long = 0,
    @Column(name = "created_at") var createdAt: Instant = Instant.now(),
    @Column(name = "resolved_at") var resolvedAt: Instant? = null,
    @Column(name = "applied_at") var appliedAt: Instant? = null,
)

@Repository
interface PolicyRecommendationEntityRepository : JpaRepository<PolicyRecommendationEntity, String> {
    fun findByResidentIdAndState(residentId: String, state: String): List<PolicyRecommendationEntity>
}

@Repository
class PolicyRecommendationRepositoryAdapter(
    private val jpa: PolicyRecommendationEntityRepository,
) : PolicyRecommendationRepository {

    override fun findById(id: RecommendationId): PolicyRecommendation? =
        jpa.findById(id.value.toString()).orElse(null)?.toDomain()

    override fun findByResidentIdAndState(residentId: ResidentId, state: RecommendationState): List<PolicyRecommendation> =
        jpa.findByResidentIdAndState(residentId.value, state.name).map { it.toDomain() }

    override fun save(recommendation: PolicyRecommendation): PolicyRecommendation =
        jpa.save(recommendation.toEntity()).toDomain()

    private fun PolicyRecommendationEntity.toDomain() = PolicyRecommendation(
        id = RecommendationId.from(id),
        episodeId = episodeId,
        residentId = ResidentId(residentId),
        title = title,
        description = description,
        presetPatch = PresetPatch(
            templateId = patchTemplateId?.let { TemplateId.from(it) },
            mode = patchMode?.let { PolicyMode.from(it) },
            riskLevel = patchRiskLevel?.let { RiskLevel.from(it) },
            mobilityAid = patchMobilityAid?.let { MobilityAid.from(it) },
            autopilot = patchAutopilot,
        ),
        origin = RecommendationOrigin.valueOf(origin),
        state = RecommendationState.valueOf(state),
        createdAt = createdAt,
        resolvedAt = resolvedAt,
        appliedAt = appliedAt,
    )

    private fun PolicyRecommendation.toEntity() = PolicyRecommendationEntity(
        id = id.value.toString(),
        episodeId = episodeId,
        residentId = residentId.value,
        title = title,
        description = description,
        origin = origin.name,
        state = state.name,
        patchTemplateId = presetPatch.templateId?.value,
        patchMode = presetPatch.mode?.name?.lowercase(),
        patchRiskLevel = presetPatch.riskLevel?.name?.lowercase(),
        patchMobilityAid = presetPatch.mobilityAid?.name?.lowercase(),
        patchAutopilot = presetPatch.autopilot,
        createdAt = createdAt,
        resolvedAt = resolvedAt,
        appliedAt = appliedAt,
    )
}
