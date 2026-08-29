package com.hub.policy.domain.repository

import com.hub.policy.domain.model.AlarmProfileVersion
import com.hub.policy.domain.model.AlarmProfileId
import com.hub.shared.domain.ResidentId

interface AlarmProfileRepository {
    fun findById(id: AlarmProfileId): AlarmProfileVersion?
    fun findCurrentByResidentId(residentId: ResidentId): AlarmProfileVersion?
    fun findByResidentId(residentId: ResidentId): List<AlarmProfileVersion>
    fun save(profile: AlarmProfileVersion): AlarmProfileVersion
    fun expireCurrentByResidentId(residentId: ResidentId)
}