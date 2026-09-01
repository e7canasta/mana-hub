package com.hub.insights.infrastructure.persistence

import com.hub.insights.domain.find.FindingPolicy
import com.hub.insights.domain.repository.FindingPolicyRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class FindingPolicyRepositoryAdapter(
    private val jpa: FindingPolicyEntityRepository,
) : FindingPolicyRepository {

    override fun findByResidentId(residentId: String): FindingPolicy? =
        jpa.findByResidentId(residentId)?.toDomain()

    override fun findDefault(): FindingPolicy? =
        jpa.findByIsDefaultTrue()?.toDomain()

    override fun save(policy: FindingPolicy): FindingPolicy {
        val entity = FindingPolicyEntity.fromDomain(policy)
        return jpa.save(entity).toDomain()
    }

    @Transactional
    override fun deleteByResidentId(residentId: String) {
        jpa.deleteByResidentId(residentId)
    }
}
