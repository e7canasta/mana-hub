package com.hub.insights.engine

import com.hub.insights.domain.derive.Baseline
import com.hub.insights.domain.derive.SleepDerived
import com.hub.insights.domain.find.Finding
import com.hub.insights.domain.recommend.Recommendation
import java.time.Instant
import java.time.LocalDate

/**
 * Contexto del pipeline de insights para un residente.
 *
 * Es el crudo que cada experto recibe — contiene todo lo que necesita
 * para generar sus hallazgos y recomendaciones.
 *
 * Otros módulos pueden importar este engine y crear sus propios expertos
 * sin modificar el core de insights.
 */
data class InsightContext(
    val residentId: String,
    val residentName: String?,
    val from: LocalDate,
    val to: LocalDate,
    val baseline: Baseline,
    val derived: SleepDerived,
    val sleepDays: List<SleepDayData>,
    val bathroomDays: List<BathroomDayData>,
    val careAvgMinutes: Double?,
    val careTotalMinutes: Int,
    val exitsLast7d: List<java.time.Instant>,
    val staffAfterExitCount: Int,
    val riskLevel: String?,
    val bedEdgeWarningMinutes: Int?,
    val relatedEpisodeIds: List<String>,
    val policyToday: List<String>,
    val episodes: List<EpisodeData>,
    val zone: java.time.ZoneId,
    val windowDays: Int,
)

// ─── Modelos del pipeline (dominio, no infra) ───

data class SleepDayData(
    val day: String,
    val calmMinutes: Int = 0,
    val restlessMinutes: Int = 0,
    val awakeMinutes: Int = 0,
    val outOfBedMinutes: Int = 0,
    val bedExitCount: Int = 0,
    val wakeCount: Int = 0,
    val measured: Boolean = true,
)

data class BathroomDayData(
    val day: String,
    val visitCount: Int = 0,
    val nightVisitCount: Int = 0,
    val assistedCount: Int = 0,
    val totalMinutes: Int = 0,
    val measured: Boolean = true,
)

data class EpisodeData(
    val id: String,
    val kind: String?,
    val severity: String?,
    val occurredAt: Instant?,
    val selfRecovery: Boolean?,
)

/**
 * Resultado de un experto — hallazgos + recomendaciones.
 *
 * Cada experto produce sus propios findings y recs.
 * El engine los ensambla.
 */
data class ExpertResult(
    val expertName: String,
    val findings: List<Finding> = emptyList(),
    val recommendations: List<Recommendation> = emptyList(),
)
