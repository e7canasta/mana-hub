package com.hub.views.readers

import com.hub.policy.domain.repository.AlarmProfileOverrideRepository
import com.hub.policy.domain.repository.AlarmProfileRepository
import com.hub.shared.domain.ResidentId
import com.hub.views.AlarmPresetsProjection
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AlarmPresetsProjectionReader(
    private val alarmProfileRepository: AlarmProfileRepository,
    private val alarmOverrideRepository: AlarmProfileOverrideRepository,
) {

    @Transactional(readOnly = true)
    fun getAlarmPresets(residentId: String): AlarmPresetsProjection {
        val version = alarmProfileRepository.findCurrentByResidentId(ResidentId(residentId))
        if (version == null) {
            return AlarmPresetsProjection(
                residentId = residentId,
                riskLevel = null, mobilityAid = null, autopilot = null,
                mode = null, templateId = null, overrides = emptyMap(),
                updatedAt = null, updatedBy = null, recommendation = null,
            )
        }
        val overrides = alarmOverrideRepository.findByProfileVersionId(version.id.value)
            .associate { override ->
                override.ruleId to when (override) {
                    is com.hub.policy.domain.model.PolicyOverride.DwellOverride ->
                        mapOf("warningAfterMinutes" to override.warningAfterMinutes, "alertAfterMinutes" to override.alertAfterMinutes)
                    is com.hub.policy.domain.model.PolicyOverride.HysteresisOverride ->
                        mapOf("hysteresisSeconds" to override.hysteresisSeconds)
                    is com.hub.policy.domain.model.PolicyOverride.ComeBackOverride ->
                        mapOf("baselineState" to override.baselineState, "alertAfterMinutes" to override.alertAfterMinutes)
                }
            }
        return AlarmPresetsProjection(
            residentId = residentId,
            riskLevel = version.riskLevel.name.lowercase(),
            mobilityAid = version.mobilityAid?.name?.lowercase(),
            autopilot = version.autopilot,
            mode = version.mode?.name?.lowercase(),
            templateId = version.templateId?.value,
            overrides = overrides,
            updatedAt = version.validFrom.toString(),
            updatedBy = version.updatedBy,
            recommendation = null,
        )
    }
}
