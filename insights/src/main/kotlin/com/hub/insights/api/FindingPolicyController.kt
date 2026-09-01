package com.hub.insights.api

import com.hub.insights.api.dto.BathroomPolicyDto
import com.hub.insights.api.dto.CarePolicyDto
import com.hub.insights.api.dto.FindingPolicyResponse
import com.hub.insights.api.dto.FindingPolicyUpdateRequest
import com.hub.insights.api.dto.SleepPolicyDto
import com.hub.insights.application.FindingPolicyService
import com.hub.insights.domain.find.BathroomPolicy
import com.hub.insights.domain.find.CarePolicy
import com.hub.insights.domain.find.SleepPolicy
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/insights/policies")
@Tag(name = "Finding Policies", description = "Configuración de reglas de análisis por residente")
class FindingPolicyController(
    private val policyService: FindingPolicyService,
) {

    @GetMapping("/default")
    @Operation(summary = "Obtener política default", description = "Retorna la política global que cubre a todos los residentes sin política propia. Si no existe, la crea con todos los valores por defecto.")
    fun getDefault(): FindingPolicyResponse =
        FindingPolicyResponse.from(policyService.getDefault())

    @PutMapping("/default")
    @Operation(summary = "Actualizar política default", description = "Actualiza la política global. Los campos no enviados conservan su valor actual.")
    fun updateDefault(
        @RequestBody body: FindingPolicyUpdateRequest,
    ): FindingPolicyResponse = FindingPolicyResponse.from(policyService.updateDefault(
        sleep = body.sleep?.toDomain(),
        care = body.care?.toDomain(),
        bathroom = body.bathroom?.toDomain(),
    ))

    @GetMapping("/{residentId}")
    @Operation(summary = "Obtener política de un residente", description = "Si el residente tiene política propia la retorna. Si no, cascada a la default.")
    fun getForResident(@PathVariable residentId: String): FindingPolicyResponse =
        FindingPolicyResponse.from(policyService.getForResident(residentId))

    @PutMapping("/{residentId}")
    @Operation(summary = "Crear o actualizar política de un residente", description = "Crea una política propia para el residente. Si ya existe, actualiza solo los campos enviados.")
    fun updateForResident(
        @PathVariable residentId: String,
        @RequestBody body: FindingPolicyUpdateRequest,
    ): FindingPolicyResponse = FindingPolicyResponse.from(policyService.updateForResident(
        residentId = residentId,
        sleep = body.sleep?.toDomain(),
        care = body.care?.toDomain(),
        bathroom = body.bathroom?.toDomain(),
    ))

    @PutMapping("/{residentId}/reset")
    @Operation(summary = "Resetear política de un residente", description = "Borra la política propia del residente. A partir de ahí cascada a la default.")
    fun resetForResident(@PathVariable residentId: String): FindingPolicyResponse =
        FindingPolicyResponse.from(policyService.resetForResident(residentId))
}

private fun SleepPolicyDto.toDomain() = SleepPolicy(
    restlessHighEnabled = restlessHighEnabled ?: true,
    restlessHighThreshold = restlessHighThreshold ?: 0.25,
    restlessFragmentedEnabled = restlessFragmentedEnabled ?: true,
    restlessFragmentedThreshold = restlessFragmentedThreshold ?: 0.35,
    exitsRisingEnabled = exitsRisingEnabled ?: true,
    exitsRisingFactor = exitsRisingFactor ?: 1.15,
    exitsRisingMinDelta = exitsRisingMinDelta ?: 0.3,
    sleepInRangeEnabled = sleepInRangeEnabled ?: true,
    sleepInRangeThreshold = sleepInRangeThreshold ?: 0.20,
    dropWoWEnabled = dropWoWEnabled ?: true,
    dropWoWMinutes = dropWoWMinutes ?: 45,
    dawnClusterEnabled = dawnClusterEnabled ?: true,
    dawnFrom = dawnFrom ?: "05:00",
    dawnTo = dawnTo ?: "06:05",
    dawnMinCount = dawnMinCount ?: 3,
    dawnRatio = dawnRatio ?: 0.66,
)

private fun CarePolicyDto.toDomain() = CarePolicy(
    careThinEnabled = careThinEnabled ?: true,
    careThinMinutes = careThinMinutes ?: 20.0,
)

private fun BathroomPolicyDto.toDomain() = BathroomPolicy(
    bathroomNightEnabled = bathroomNightEnabled ?: true,
    nightMinAvg = nightMinAvg ?: 1.0,
    nightRiseFactor = nightRiseFactor ?: 1.5,
)
