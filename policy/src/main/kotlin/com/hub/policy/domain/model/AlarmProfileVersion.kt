package com.hub.policy.domain.model

import com.hub.shared.domain.AggregateRoot
import com.hub.shared.domain.ResidentId
import java.time.Instant

class AlarmProfileVersion private constructor(
    override val id: AlarmProfileId,
    val residentId: ResidentId,
    val validFrom: Instant,
    val validTo: Instant?,
    val mobilityAid: String?,
    val autopilot: Boolean,
    val mode: String?,
    val templateId: String?,
    val overridesJson: String,
    val catalogVersion: String?,
    val updatedBy: String?,
    val riskLevel: RiskLevel,
    override var version: Long
) : AggregateRoot<AlarmProfileId>() {

    val isCurrent: Boolean get() = validTo == null

    fun expire(): AlarmProfileVersion {
        require(isCurrent) { "Profile is not current" }
        return reconstitute(
            id = id, residentId = residentId, validFrom = validFrom, validTo = Instant.now(),
            mobilityAid = mobilityAid, autopilot = autopilot, mode = mode, templateId = templateId,
            overridesJson = overridesJson, catalogVersion = catalogVersion, updatedBy = updatedBy,
            riskLevel = riskLevel, version = version + 1
        )
    }

    fun update(
        mobilityAid: String?, autopilot: Boolean?, mode: String?, templateId: String?,
        overridesJson: String?, riskLevel: RiskLevel?, updatedBy: String?
    ): AlarmProfileVersion {
        require(isCurrent) { "Profile is not current" }
        return reconstitute(
            id = id, residentId = residentId, validFrom = validFrom, validTo = validTo,
            mobilityAid = mobilityAid ?: this.mobilityAid,
            autopilot = autopilot ?: this.autopilot,
            mode = mode ?: this.mode,
            templateId = templateId ?: this.templateId,
            overridesJson = overridesJson ?: this.overridesJson,
            catalogVersion = catalogVersion,
            updatedBy = updatedBy ?: this.updatedBy,
            riskLevel = riskLevel ?: this.riskLevel,
            version = version + 1
        )
    }

    companion object {
        fun create(residentId: ResidentId, updatedBy: String?): AlarmProfileVersion = AlarmProfileVersion(
            id = AlarmProfileId.random(), residentId = residentId, validFrom = Instant.now(),
            validTo = null, mobilityAid = null, autopilot = false, mode = null, templateId = null,
            overridesJson = "{}", catalogVersion = null, updatedBy = updatedBy, riskLevel = RiskLevel.MEDIUM, version = 0
        )

        fun reconstitute(
            id: AlarmProfileId, residentId: ResidentId, validFrom: Instant, validTo: Instant?,
            mobilityAid: String?, autopilot: Boolean, mode: String?, templateId: String?,
            overridesJson: String, catalogVersion: String?, updatedBy: String?,
            riskLevel: RiskLevel, version: Long
        ): AlarmProfileVersion = AlarmProfileVersion(
            id, residentId, validFrom, validTo, mobilityAid, autopilot, mode, templateId,
            overridesJson, catalogVersion, updatedBy, riskLevel, version
        )
    }
}
