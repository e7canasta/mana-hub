package com.hub.clients.simulation

import com.hub.clients.core.ManaHubScope
import com.hub.clients.observation.BathroomSummaryData
import com.hub.clients.observation.MobilitySummaryData
import com.hub.clients.observation.SleepSummaryData
import java.time.Instant
import java.time.LocalDate

/**
 * Rol externo: ObservationEngine
 *
 * Responsabilidad: Detecta cambios de estado en escenas/cámaras.
 * Ejemplo: Servicio de visión por computadora que analiza video.
 *
 * Este componente:
 * - Recibe eventos crudos de cámaras/sensores
 * - Detecta transiciones de estado (sleeping → out_of_bed)
 * - Calcula resúmenes diarios (sueño, movilidad, baño)
 *
 * NO es parte de mana-hub. Es un consumidor de nuestro SOR.
 */
interface ObservationEngine {
    fun detectBedExit(bedId: String, residentId: String, monitorKey: String)
    fun detectMotion(bedId: String, residentId: String, monitorKey: String, state: String)
    fun detectWandering(bedId: String, residentId: String, monitorKey: String)
    fun computeSleepSummary(residentId: String, date: LocalDate, data: SleepSummaryData)
    fun computeMobilitySummary(residentId: String, date: LocalDate, data: MobilitySummaryData)
    fun computeBathroomSummary(residentId: String, date: LocalDate, data: BathroomSummaryData)
}

/**
 * Rol externo: EpisodeEngine
 *
 * Responsabilidad: Evalúa reglas y decide si dispara episodios.
 * Ejemplo: Motor de reglas de negocio que evalúa umbrales.
 *
 * Este componente:
 * - Consulta el catálogo de presets para conocer umbrales
 * - Consulta el perfil del residente para saber qué preset tiene
 * - Evalúa reglas basándose en el preset + overrides
 * - Decide si dispara un episodio
 *
 * NO es parte de mana-hub. Es un consumidor de nuestro SOR.
 */
interface EpisodeEngine {
    fun evaluateAndTrigger(residentId: String, bedId: String, event: String)
}

/**
 * Rol externo: NotificationService
 *
 * Responsabilidad: Envía notificaciones (SMS, push, email).
 * Ejemplo: Servicio de mensajería como Twilio, Firebase, etc.
 *
 * Este componente:
 * - Consulta el detalle del episodio
 * - Decide a quién notificar
 * - Envía la notificación
 * - Registra que la notificación fue enviada
 *
 * NO es parte de mana-hub. Es un consumidor de nuestro SOR.
 */
interface NotificationService {
    fun notifyStaff(episodeId: String, recipientId: String, channel: String, message: String)
    fun notifySupervisor(episodeId: String, recipientId: String, channel: String, message: String)
}

/**
 * Rol externo: EvidenceCollector
 *
 * Responsabilidad: Recopila evidencia (video, clips, fotos).
 * Ejemplo: Servicio de grabación de video.
 *
 * Este componente:
 * - Detecta eventos que requieren evidencia
 * - Recorta clips de video relevantes
 * - Registra la evidencia en mana-hub
 *
 * NO es parte de mana-hub. Es un consumidor de nuestro SOR.
 */
interface EvidenceCollector {
    fun collectVideoClip(bedId: String, residentId: String, startTime: Instant, endTime: Instant)
    fun collectTimeline(bedId: String, residentId: String)
}
