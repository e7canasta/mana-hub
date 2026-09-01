package com.hub.insights.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FindingPolicyEntityRepository : JpaRepository<FindingPolicyEntity, String> {
    fun findByResidentId(residentId: String): FindingPolicyEntity?
    fun findByIsDefaultTrue(): FindingPolicyEntity?
}
