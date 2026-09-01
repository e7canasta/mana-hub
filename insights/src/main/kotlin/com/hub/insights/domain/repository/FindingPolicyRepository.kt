package com.hub.insights.domain.repository

import com.hub.insights.domain.find.FindingPolicy

interface FindingPolicyRepository {
    fun findByResidentId(residentId: String): FindingPolicy?
    fun findDefault(): FindingPolicy?
    fun save(policy: FindingPolicy): FindingPolicy
}
