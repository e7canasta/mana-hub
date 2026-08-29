package com.hub.policy.domain.repository

import com.hub.policy.domain.model.PolicyOverride

interface AlarmProfileOverrideRepository {
    fun findByProfileVersionId(profileVersionId: String): List<PolicyOverride>
    fun saveAll(overrides: List<PolicyOverride>, profileVersionId: String)
    fun deleteByProfileVersionId(profileVersionId: String)
}
