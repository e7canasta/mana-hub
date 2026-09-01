package com.hub.policy.domain.model

import com.hub.shared.domain.AggregateRoot
import com.hub.shared.domain.ResidentId
import com.hub.policy.domain.event.AlarmProfileEvent
import java.time.Instant

data class AlarmProfileVersion private constructor(
    override val id: AlarmProfileId,
    val residentId: ResidentId,
    val validFrom: Instant,
    val validTo: Instant?,
    val mobilityAid: MobilityAid?,
    val autopilot: Boolean,
    val mode: PolicyMode?,
    val templateId: TemplateId?,
    val catalogVersion: String?,
    val updatedBy: String?,
    val riskLevel: RiskLevel,
    override var version: Long
) : AggregateRoot<AlarmProfileId>() {

    private val _domainEvents = mutableListOf<AlarmProfileEvent>()
    val domainEvents: List<AlarmProfileEvent> get() = _domainEvents.toList()
    fun clearEvents() = _domainEvents.clear()

    val isCurrent: Boolean get() = validTo == null

    fun expire(): AlarmProfileVersion {
        require(isCurrent) { "Profile is not current" }
        val next = copy(validTo = Instant.now(), version = version + 1)
        next._domainEvents.add(
            AlarmProfileEvent.Expired(profileId = id, residentId = residentId)
        )
        return next
    }

    fun update(
        mobilityAid: MobilityAid?, autopilot: Boolean?, mode: PolicyMode?, templateId: TemplateId?,
        riskLevel: RiskLevel?, updatedBy: String?
    ): AlarmProfileVersion {
        require(isCurrent) { "Profile is not current" }
        val next = copy(
            mobilityAid = mobilityAid ?: this.mobilityAid,
            autopilot = autopilot ?: this.autopilot,
            mode = mode ?: this.mode,
            templateId = templateId ?: this.templateId,
            updatedBy = updatedBy ?: this.updatedBy,
            riskLevel = riskLevel ?: this.riskLevel,
            version = version + 1
        )
        next._domainEvents.add(
            AlarmProfileEvent.Updated(
                profileId = id, residentId = residentId,
                riskLevel = next.riskLevel, updatedBy = next.updatedBy,
            )
        )
        return next
    }

    companion object {
        fun create(residentId: ResidentId, updatedBy: String?): AlarmProfileVersion {
            val profile = AlarmProfileVersion(
                id = AlarmProfileId.random(), residentId = residentId, validFrom = Instant.now(),
                validTo = null, mobilityAid = MobilityAid.NONE, autopilot = false, mode = PolicyMode.PRESET, templateId = null,
                catalogVersion = null, updatedBy = updatedBy, riskLevel = RiskLevel.MEDIUM, version = 0
            )
            profile._domainEvents.add(
                AlarmProfileEvent.Created(profileId = profile.id, residentId = residentId, updatedBy = updatedBy)
            )
            return profile
        }

        fun reconstitute(
            id: AlarmProfileId, residentId: ResidentId, validFrom: Instant, validTo: Instant?,
            mobilityAid: MobilityAid?, autopilot: Boolean, mode: PolicyMode?, templateId: TemplateId?,
            catalogVersion: String?, updatedBy: String?, riskLevel: RiskLevel, version: Long
        ) = AlarmProfileVersion(
            id = id, residentId = residentId, validFrom = validFrom, validTo = validTo,
            mobilityAid = mobilityAid, autopilot = autopilot, mode = mode, templateId = templateId,
            catalogVersion = catalogVersion, updatedBy = updatedBy, riskLevel = riskLevel, version = version
        )
    }
}
