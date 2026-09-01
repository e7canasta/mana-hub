package com.hub.insights.experts

import com.hub.insights.domain.recommend.WellbeingRecommendations
import com.hub.insights.engine.ExpertResult
import com.hub.insights.engine.InsightContext

/**
 * Experto de cuidado — analiza la actividad de enfermería y cuidado.
 *
 * Detecta:
 * - Poco cuidado medido en la ventana (CARE_THIN)
 * - Recomendaciones de cuidado basadas en línea base
 */
class CareExpert : Expert {

    override fun evaluate(ctx: InsightContext): ExpertResult {
        val baseline = com.hub.insights.domain.derive.Baseline(
            admissionDate = null,
            observedFrom = ctx.baseline.observedFrom,
            observedDays = ctx.baseline.observedDays,
            ready = ctx.baseline.ready,
        )
        val recommendations = WellbeingRecommendations.forCare(
            baseline = baseline,
            avgMinutes = ctx.careAvgMinutes,
            totalMinutes = ctx.careTotalMinutes,
        )

        return ExpertResult(
            expertName = "cuidado",
            recommendations = recommendations,
        )
    }
}
