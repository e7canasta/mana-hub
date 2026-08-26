package com.hub.policy.application.service

import com.hub.policy.application.dto.*
import com.hub.policy.domain.model.*
import com.hub.policy.domain.repository.AlarmProfileRepository
import com.hub.population.domain.model.ResidentId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AlarmProfileApplicationService(
    private val alarmProfileRepository: AlarmProfileRepository
) {

    @Transactional(readOnly = true)
    fun getResidentProfile(residentId: String): AlarmProfileResponse? {
        return alarmProfileRepository.findCurrentByResidentId(ResidentId(residentId))?.toResponse()
    }

    @Transactional
    fun updateResidentProfile(residentId: String, request: UpdateAlarmProfileRequest): AlarmProfileResponse {
        val rid = ResidentId(residentId)
        alarmProfileRepository.expireCurrentByResidentId(rid)

        val newProfile = AlarmProfileVersion.create(rid, request.updatedBy).update(
            mobilityAid = request.mobilityAid, autopilot = request.autopilot,
            mode = request.mode, templateId = request.templateId,
            overridesJson = request.overridesJson, riskLevel = request.riskLevel,
            updatedBy = request.updatedBy
        )

        return alarmProfileRepository.save(newProfile).toResponse()
    }

    @Transactional(readOnly = true)
    fun getProfileHistory(residentId: String): List<AlarmProfileResponse> {
        return alarmProfileRepository.findByResidentId(ResidentId(residentId)).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getCatalog(): AlarmPresetCatalogResponse {
        return AlarmPresetCatalogResponse(presets = listOf(
            AlarmPresetDefinition(
                id = "default",
                name = "Monitoreo General",
                description = "Reglas estándar para todos los residentes",
                thresholds = mapOf(
                    "bedExitAlertMinutes" to 10,
                    "bathroomVisitNightMax" to 5,
                    "outOfBedAlertDelay" to 15
                )
            ),
            AlarmPresetDefinition(
                id = "fall_risk",
                name = "Riesgo de Caída",
                description = "Residentes con alto riesgo de caídas",
                thresholds = mapOf(
                    "bedExitAlertMinutes" to 5,
                    "bathroomVisitNightMax" to 3,
                    "outOfBedAlertDelay" to 2,
                    "fallDetectionSensitivity" to "high"
                )
            ),
            AlarmPresetDefinition(
                id = "wanderer",
                name = "Deambulación",
                description = "Residentes con tendencia a deambular",
                thresholds = mapOf(
                    "bedExitAlertMinutes" to 3,
                    "hallwayZoneAlert" to true,
                    "exitZoneAlert" to true,
                    "nightMovementAlert" to true
                )
            ),
            AlarmPresetDefinition(
                id = "night_watch",
                name = "Vigilia Nocturna",
                description = "Monitoreo reforzado en horario nocturno",
                thresholds = mapOf(
                    "bedExitAlertMinutes" to 3,
                    "bathroomVisitNightMax" to 2,
                    "nightMovementAlert" to true,
                    "quietHoursStart" to "22:00",
                    "quietHoursEnd" to "06:00"
                )
            )
        ))
    }

    private fun AlarmProfileVersion.toResponse() = AlarmProfileResponse(
        id = id.value, residentId = residentId.value, validFrom = validFrom, validTo = validTo,
        mobilityAid = mobilityAid, autopilot = autopilot, mode = mode, templateId = templateId,
        overridesJson = overridesJson, riskLevel = riskLevel, isCurrent = isCurrent
    )
}