package com.hub.insights.api.dto

import com.hub.insights.domain.find.BathroomPolicy
import com.hub.insights.domain.find.CarePolicy
import com.hub.insights.domain.find.FindingPolicy
import com.hub.insights.domain.find.SleepPolicy
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Política de reglas de análisis de hallazgos")
data class FindingPolicyResponse(
    @field:Schema(description = "Identificador único")
    val id: String,
    @field:Schema(description = "ID del residente. Null para la política default")
    val residentId: String?,
    @field:Schema(description = "true si es la política global que cubre a todos", name = "default")
    val isDefault: Boolean,
    @field:Schema(description = "Umbrales de evaluación de sueño")
    val sleep: SleepPolicy,
    @field:Schema(description = "Umbrales de evaluación de cuidado")
    val care: CarePolicy,
    @field:Schema(description = "Umbrales de evaluación de visitas nocturnas")
    val bathroom: BathroomPolicy,
    @field:Schema(description = "Versión para optimistic locking")
    val version: Long,
) {
    companion object {
        fun from(domain: FindingPolicy) = FindingPolicyResponse(
            id = domain.id,
            residentId = domain.residentId,
            isDefault = domain.isDefault,
            sleep = domain.sleep,
            care = domain.care,
            bathroom = domain.bathroom,
            version = domain.version,
        )
    }
}
