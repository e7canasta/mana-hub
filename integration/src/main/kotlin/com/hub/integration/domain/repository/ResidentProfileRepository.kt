package com.hub.integration.domain.repository

import com.hub.integration.domain.model.ResidentProfile

interface ResidentProfileRepository {
    fun findCurrentByResidentId(residentId: String): ResidentProfile?
    fun findByResidentId(residentId: String): List<ResidentProfile>
    fun save(profile: ResidentProfile): ResidentProfile
    fun saveAndFlush(profile: ResidentProfile): ResidentProfile
    fun findActiveProfiles(): List<ResidentProfile>
}
