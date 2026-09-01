package com.hub.insights.experts

import com.hub.insights.domain.find.FindingCatalog
import com.hub.insights.domain.find.FindingContext
import com.hub.insights.domain.find.SleepBriefing
import com.hub.insights.domain.recommend.WellbeingRecommendations
import com.hub.insights.engine.ExpertResult
import com.hub.insights.engine.InsightContext

/**
 * Experto de sueño — analiza patrones de descanso del residente.
 *
 * Detecta:
 * - Salidas de cama en ventana de alba (cluster de alba)
 * - Sueño inquieto por encima de lo habitual
 * - Salidas de cama en aumento
 * - Visitas nocturnas al baño en aumento
 * - Sueño dentro/fuera del rango habitual
 *
 * Los umbrales vienen de SleepPolicy (configurable por instalación).
 */
class SleepExpert : Expert {

    override fun evaluate(ctx: InsightContext): ExpertResult {
        val findingCtx = FindingContext(
            residentId = ctx.residentId,
            residentName = ctx.residentName,
            baseline = ctx.baseline,
            sleep = ctx.derived,
            sleepDays = ctx.sleepDays.map {
                com.hub.insights.inbound.HubSleepDay(
                    day = it.day,
                    calmMinutes = it.calmMinutes,
                    restlessMinutes = it.restlessMinutes,
                    awakeMinutes = it.awakeMinutes,
                    outOfBedMinutes = it.outOfBedMinutes,
                    bedExitCount = it.bedExitCount,
                    wakeCount = it.wakeCount,
                    measured = it.measured,
                )
            },
            bathroomDays = ctx.bathroomDays.map {
                com.hub.insights.inbound.HubBathroomDay(
                    day = it.day,
                    visitCount = it.visitCount,
                    nightVisitCount = it.nightVisitCount,
                    assistedCount = it.assistedCount,
                    totalMinutes = it.totalMinutes,
                    measured = it.measured,
                )
            },
            careAvgMinutes = ctx.careAvgMinutes,
            exitsLast7d = ctx.exitsLast7d,
            staffAfterExitCount = ctx.staffAfterExitCount,
            riskLevel = ctx.riskLevel,
            bedEdgeWarningMinutes = ctx.bedEdgeWarningMinutes,
            zone = ctx.zone,
            windowDays = ctx.windowDays,
            relatedEpisodeIds = ctx.relatedEpisodeIds,
        )

        val findings = FindingCatalog.evaluate(
            ctx = findingCtx,
            sleepPolicy = ctx.sleepPolicy,
            bathroomPolicy = ctx.bathroomPolicy,
        )
        val baseline = com.hub.insights.domain.derive.Baseline(
            admissionDate = null,
            observedFrom = ctx.baseline.observedFrom,
            observedDays = ctx.baseline.observedDays,
            ready = ctx.baseline.ready,
        )
        val recommendations = WellbeingRecommendations.forSleep(baseline, ctx.derived, ctx.sleepPolicy)

        return ExpertResult(
            expertName = "sueño",
            findings = findings,
            recommendations = recommendations,
        )
    }
}
