package com.hub.insights.engine

import com.hub.insights.experts.CareExpert
import com.hub.insights.experts.Expert
import com.hub.insights.experts.PolicyExpert
import com.hub.insights.experts.SleepExpert
import com.hub.insights.domain.find.Finding
import com.hub.insights.domain.recommend.Recommendation

/**
 * Motor de insights — coordina los cabezales expertos.
 *
 * Recibe el contexto del pipeline (crudo de los repos del dominio)
 * y ejecuta cada experto en secuencia. Los resultados se ensamblan
 * en una lista unificada de hallazgos y recomendaciones.
 *
 * Importable por otros módulos — pueden crear sus propios expertos
 * y registrarlos sin modificar el core.
 */
class InsightEngine(
    private val experts: List<Expert> = defaultExperts(),
) {

    fun evaluate(ctx: InsightContext): EngineResult {
        val results = experts.map { it.evaluate(ctx) }
        return EngineResult(
            residentId = ctx.residentId,
            findings = results.flatMap { it.findings },
            recommendations = results.flatMap { it.recommendations },
            expertResults = results,
        )
    }

    companion object {
        fun defaultExperts(): List<Expert> = listOf(
            SleepExpert(),
            CareExpert(),
            PolicyExpert(),
        )
    }
}

/**
 * Resultado consolidado del motor de insights.
 *
 * Contiene todos los hallazgos y recomendaciones de todos los expertos,
 * plus el desglose por experto para debugging o visualización.
 */
data class EngineResult(
    val residentId: String,
    val findings: List<Finding>,
    val recommendations: List<Recommendation>,
    val expertResults: List<ExpertResult>,
)
