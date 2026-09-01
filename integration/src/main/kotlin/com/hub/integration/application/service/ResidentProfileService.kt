package com.hub.integration.application.service

import com.hub.integration.domain.model.ResidentProfile
import com.hub.integration.domain.repository.ResidentProfileRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Profile integration — receives ResidentProfileDto from mana-hive,
 * persists it, and notifies the bridge.
 */
@Service
class ResidentProfileService(
    private val repository: ResidentProfileRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {

    @Transactional
    fun ingestProfile(rawJson: String): ResidentProfile {
        val profile = ResidentProfile.fromRawJson(rawJson)

        // Idempotency: same (residentId, version) already persisted
        val existing = repository.findByResidentId(profile.residentId)
        existing.find { it.version == profile.version }?.let {
            log.info("Profile v{} for {} already exists, skipping", profile.version, profile.residentId)
            return it
        }

        // Save new profile (flush inside try so constraint violation is caught in-transaction)
        val saved = try {
            repository.saveAndFlush(profile)
        } catch (e: org.springframework.dao.DataIntegrityViolationException) {
            log.info("Profile v{} for {} already exists (race condition), fetching", profile.version, profile.residentId)
            repository.findByResidentId(profile.residentId).find { it.version == profile.version }
                ?: throw e
        }
        log.info("Profile saved: {} v{} for {}", profile.profileId, profile.version, profile.residentId)

        // Notify bridge (non-blocking)
        try {
            eventPublisher.publishEvent(ProfileChangedEvent(profile.residentId, rawJson))
            log.info("Bridge notified of profile change for {}", profile.residentId)
        } catch (e: Exception) {
            log.warn("Failed to notify bridge for {}: {}", profile.residentId, e.message)
        }

        return saved!!
    }

    @Transactional(readOnly = true)
    fun getCurrent(residentId: String): ResidentProfile? =
        repository.findCurrentByResidentId(residentId)

    @Transactional(readOnly = true)
    fun getActiveProfiles(): List<ResidentProfile> =
        repository.findActiveProfiles()

    companion object {
        private val log = LoggerFactory.getLogger(ResidentProfileService::class.java)
    }
}

data class ProfileChangedEvent(
    val residentId: String,
    val rawJson: String,
)
