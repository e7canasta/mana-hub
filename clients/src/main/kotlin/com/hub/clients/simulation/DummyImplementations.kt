package com.hub.clients.simulation

import com.hub.clients.core.ManaHubScope
import com.hub.clients.observation.BathroomSummaryData
import com.hub.clients.observation.MobilitySummaryData
import com.hub.clients.observation.PerceptionKind
import com.hub.clients.observation.SleepSummaryData
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Implementación dummy del ObservationEngine.
 *
 * Simula un motor de detección por visión computarizada.
 * Usa nuestro DSL para persistir los eventos en mana-hub.
 *
 * En producción, este componente sería reemplazado por un servicio real
 * de análisis de video/cámaras.
 */
class DummyObservationEngine(private val hub: ManaHubScope) : ObservationEngine {

    override fun detectBedExit(bedId: String, residentId: String, monitorKey: String) {
        println("   [ObservationEngine] Detectando bed_exit en $monitorKey")
        hub.observation.ingestEvent(
            monitorKey = monitorKey,
            kind = PerceptionKind.LOCATION,
            bedId = bedId,
            residentId = residentId,
            state = "out_of_bed",
            sleeping = false
        )
    }

    override fun detectMotion(bedId: String, residentId: String, monitorKey: String, state: String) {
        println("   [ObservationEngine] Detectando motion: $state en $monitorKey")
        hub.observation.ingestEvent(
            monitorKey = monitorKey,
            kind = PerceptionKind.POSTURE,
            bedId = bedId,
            residentId = residentId,
            state = state
        )
    }

    override fun detectWandering(bedId: String, residentId: String, monitorKey: String) {
        println("   [ObservationEngine] Detectando wandering en $monitorKey")
        hub.observation.ingestEvent(
            monitorKey = monitorKey,
            kind = PerceptionKind.LOCATION,
            bedId = bedId,
            residentId = residentId,
            state = "out_of_sight"
        )
    }

    override fun computeSleepSummary(residentId: String, date: LocalDate, data: SleepSummaryData) {
        println("   [ObservationEngine] computing sleep summary para $residentId")
        hub.observation.ingestSleepSummary(residentId, date, data)
    }

    override fun computeMobilitySummary(residentId: String, date: LocalDate, data: MobilitySummaryData) {
        println("   [ObservationEngine] computing mobility summary para $residentId")
        hub.observation.ingestMobilitySummary(residentId, date, data)
    }

    override fun computeBathroomSummary(residentId: String, date: LocalDate, data: BathroomSummaryData) {
        println("   [ObservationEngine] computing bathroom summary para $residentId")
        hub.observation.ingestBathroomSummary(residentId, date, data)
    }
}

/**
 * Implementación dummy del EpisodeEngine.
 *
 * Simula un motor de reglas que evalúa si se debe disparar un episodio.
 * Usa nuestro DSL para:
 * 1. Consultar el catálogo de presets (conocer umbrales)
 * 2. Consultar el perfil del residente (saber qué preset tiene)
 * 3. Evaluar reglas basándose en preset + overrides
 * 4. Crear episodios en mana-hub
 *
 * En producción, este componente sería reemplazado por un motor de reglas
 * como Droplets, Apache Camel, o un servicio personalizado.
 */
class DummyEpisodeEngine(private val hub: ManaHubScope) : EpisodeEngine {

    override fun evaluateAndTrigger(residentId: String, bedId: String, event: String) {
        println("   [EpisodeEngine] Evaluando evento: $event")

        // 1. Consultar perfil del residente
        val profile = hub.policy.alarmProfile(residentId)
        val presetId = profile?.templateId ?: "default"
        println("   [EpisodeEngine] Perfil del residente: preset=$presetId, risk=${profile?.riskLevel}")

        // 2. Consultar catálogo para conocer umbrales del preset
        val preset = hub.policy.presetById(presetId)
        if (preset != null) {
            println("   [EpisodeEngine] Umbrales del preset '${preset.name}':")
            preset.thresholds.forEach { (key, value) ->
                println("     - $key: $value")
            }
        }

        // 3. Evaluar regla (simplificado)
        val severity = when {
            event == "bed_exit" -> "WARNING"
            event == "wandering" -> "CRITICAL"
            event == "fall_detected" -> "EMERGENCY"
            else -> "INFO"
        }

        val title = when (event) {
            "bed_exit" -> "Residente fuera de cama"
            "wandering" -> "Residente deambulando"
            "fall_detected" -> "Caída detectada"
            else -> "Evento detectado"
        }

        // 4. Disparar episodio
        println("   [EpisodeEngine] Disparando episodio: $severity - $title")
        hub.surveillance.triggerEpisode(residentId) {
            this.bedId = bedId
            this.severity = com.hub.clients.surveillance.EpisodeSeverity.valueOf(severity)
            this.title = title
            this.occurredAt = Instant.now()
        }
    }
}

/**
 * Implementación dummy del NotificationService.
 *
 * Simula un servicio de notificaciones que envía SMS/push.
 * Usa nuestro DSL para consultar episodios y registrar notificaciones.
 *
 * En producción, este componente sería reemplazado por un servicio real
 * como Twilio (SMS), Firebase (push), o SendGrid (email).
 */
class DummyNotificationService(private val hub: ManaHubScope) : NotificationService {

    override fun notifyStaff(episodeId: String, recipientId: String, channel: String, message: String) {
        println("   [NotificationService] Enviando $channel a staff $recipientId: $message")
        // En producción, aquí se enviaría el SMS/push real
    }

    override fun notifySupervisor(episodeId: String, recipientId: String, channel: String, message: String) {
        println("   [NotificationService] Enviando $channel a supervisor $recipientId: $message")
    }
}

/**
 * Implementación dummy del EvidenceCollector.
 *
 * Simula un servicio de recopilación de evidencia.
 * Usa nuestro DSL para registrar evidencia en mana-hub.
 *
 * En producción, este componente sería reemplazado por un servicio real
 * de grabación de video que recorta clips basados en eventos.
 */
class DummyEvidenceCollector(private val hub: ManaHubScope) : EvidenceCollector {

    override fun collectVideoClip(bedId: String, residentId: String, startTime: Instant, endTime: Instant) {
        println("   [EvidenceCollector] Recopilando clip de video: $startTime → $endTime")
        hub.evidence.createEvidence(bedId, residentId, "video_clip", "auto_collected")
        hub.evidence.openClipWindow(bedId, residentId)
    }

    override fun collectTimeline(bedId: String, residentId: String) {
        println("   [EvidenceCollector] Abriendo timeline para bed $bedId")
        hub.evidence.openTimeline(bedId, residentId)
    }
}
