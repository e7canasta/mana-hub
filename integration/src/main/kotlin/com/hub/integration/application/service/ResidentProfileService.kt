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
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun ingestProfile(rawJson: String): ResidentProfile {
        val profile = ResidentProfile.fromRawJson(rawJson)

        // Check if same version already exists
        val existing = repository.findByResidentId(profile.residentId)
        val current = existing.find { it.supersedes == null }

        if (current != null && current.version == profile.version) {
            log.info("Profile v{} for {} already exists, skipping", profile.version, profile.residentId)
            return current
        }

        // Expire current profile if new version is higher
        if (current != null && current.version < profile.version) {
            val expired = current.copy(supersedes = profile.version - 1)
            repository.save(expired)
            log.info("Expired profile v{} for {}", current.version, profile.residentId)
        }

        // Save new profile
        val saved = repository.save(profile)
        log.info("Profile saved: {} v{} for {}", profile.profileId, profile.version, profile.residentId)

        // Notify bridge
        eventPublisher.publishEvent(ProfileChangedEvent(profile.residentId, rawJson))

        return saved
    }

    @Transactional(readOnly = true)
    fun getCurrent(residentId: String): ResidentProfile? =
        repository.findCurrentByResidentId(residentId)

    @Transactional(readOnly = true)
    fun getActiveProfiles(): List<ResidentProfile> =
        repository.findActiveProfiles()
}

data class ProfileChangedEvent(
    val residentId: String,
    val rawJson: String,
)
