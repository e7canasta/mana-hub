package com.hub.clients.panel

import com.hub.panel.dto.*
import com.manahive.contracts.policy.RiskLevel

fun main() {
    panel("http://localhost:8080") {
        // ─── Episodios ─────────────────────────────
        val feed = episodes.feed()
        println("Episodios: ${feed.summary.total} total, ${feed.summary.pending} pendientes")

        feed.episodes.take(3).forEach { ep ->
            println("  ${ep.severity.value} · ${ep.residentName} · ${ep.title} · ${ep.openedAt}")
        }

        // ─── Detalle de un episodio ────────────────
        if (feed.episodes.isNotEmpty()) {
            val detail = episodes.detail(feed.episodes.first().id)
            println("\nTimeline de ${detail.id}:")
            detail.timeline.forEach { item ->
                println("  ${item.at} [${item.type}] ${item.description ?: item.eventType ?: item.signalType}")
            }
        }

        // ─── Revisar episodio ──────────────────────
        if (feed.episodes.isNotEmpty()) {
            val first = feed.episodes.first()
            if (first.verdict == null) {
                episodes.review(first.id, EpisodeVerdict.CONFIRMED, "Caída real", "gaston")
                println("\nEpisodio ${first.id} revisado como CONFIRMED")
            }
        }

        // ─── Preferencias ──────────────────────────
        val prefs = preferences.list()
        println("\nPreferencias: ${prefs.size} residentes")
        prefs.forEach { p ->
            println("  ${p.residentName} · nivel=${p.riskLevel.name} · autopilot=${p.autopilot}")
        }

        // ─── Catálogo ──────────────────────────────
        val catalog = preferences.catalog()
        println("\nCatálogo: ${catalog.transitions.size} transiciones, ${catalog.levels.size} niveles")
        catalog.transitions.forEach { t ->
            println("  ${t.id.value} · ${t.label} · locked=${t.locked} · requiresAid=${t.requiresAid?.name ?: "-"}")
        }

        // ─── Guardar preferencias ──────────────────
        if (prefs.isNotEmpty()) {
            val first = prefs.first()
            preferences.save(first.residentId) {
                riskLevel = RiskLevel.HIGH
                reason = "Tras 3 caídas en 7 días"
                updatedBy = "gaston"
            }
            println("\nPreferencias de ${first.residentName} actualizadas a HIGH")
        }
    }
}
