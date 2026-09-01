package com.hub.insights.domain.find

/**
 * Política de evaluación de hallazgos — compuesta por las tres policies
 * de dominio. Equivalente funcional a AlarmProfileVersion para monitoreo,
 * pero para la capa de insights.
 *
 * Cada residente puede tener su propia FindingPolicy. Si no tiene,
 * se usa la default (isDefault = true). La default cubre todos
 * los residentes que no tienen política propia.
 *
 * Cada regla se puede prender/apagar individualmente por residente.
 */
data class FindingPolicy(
    val id: String = "",
    val residentId: String? = null,
    val isDefault: Boolean = false,
    val sleep: SleepPolicy = SleepPolicy(),
    val care: CarePolicy = CarePolicy(),
    val bathroom: BathroomPolicy = BathroomPolicy(),
    val version: Long = 0,
)
