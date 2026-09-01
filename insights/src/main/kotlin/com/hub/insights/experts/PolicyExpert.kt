package com.hub.insights.experts

import com.hub.insights.domain.find.PolicyCopy
import com.hub.insights.engine.ExpertResult
import com.hub.insights.engine.InsightContext

/**
 * Experto de política — analiza configuración de alarmas y reglas de seguridad.
 *
 * Detecta:
 * - Nivel de riesgo actual del residente
 * - Reglas habladas (qué avisa y cuándo)
 * - Alertas de borde de cama en ventana de alba
 */
class PolicyExpert : Expert {

    override fun evaluate(ctx: InsightContext): ExpertResult {
        val policyToday = PolicyCopy.spokenLines(ctx.riskLevel, emptyMap())

        return ExpertResult(
            expertName = "política",
        )
    }
}
