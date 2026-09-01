package com.hub.insights.experts

import com.hub.insights.engine.ExpertResult
import com.hub.insights.engine.InsightContext

/**
 * Interfaz de un cabezal experto.
 *
 * Cada experto conoce un dominio del bienestar del residente.
 * Recibe el contexto del pipeline y genera sus hallazgos y recomendaciones.
 *
 * Otros módulos pueden implementar esta interfaz para crear
 * expertos adicionales sin modificar el core de insights.
 */
fun interface Expert {
    fun evaluate(ctx: InsightContext): ExpertResult
}
