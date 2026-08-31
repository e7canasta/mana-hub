package com.hub.admin

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.hub.integration.infrastructure.persistence.ResidentProfileEntityRepository
import com.hub.observation.infrastructure.persistence.SceneEventEntityRepository
import com.hub.observation.infrastructure.persistence.SentinelSignalEntityRepository
import com.hub.surveillance.infrastructure.persistence.EpisodeEntityRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin")
class CleanController(
    private val episodeJpa: EpisodeEntityRepository,
    private val sceneJpa: SceneEventEntityRepository,
    private val signalJpa: SentinelSignalEntityRepository,
    private val profileJpa: ResidentProfileEntityRepository,
) {
    private val mapper = ObjectMapper().apply { registerModule(JavaTimeModule()) }
    private fun parseJsonOrRaw(json: String): Any = try {
        mapper.readValue(json, object : TypeReference<Map<String, Any>>() {})
    } catch (_: Exception) { json }
    private fun parseJsonOrRawNullable(json: String?): Any? = json?.let { parseJsonOrRaw(it) }
    @PostMapping("/clean")
    fun clean(
        @RequestParam residentId: String,
        @RequestParam(required = false) bedId: String? = null,
        @RequestParam(required = false, defaultValue = "false") cleanProfiles: Boolean = false,
    ): ResponseEntity<Map<String, Any>> {
        val episodes = episodeJpa.findByResidentId(residentId)
        val deletedEpisodes = episodes.size
        episodeJpa.deleteAll(episodes)
        val scenes = if (bedId != null) sceneJpa.findByBedId(bedId) else sceneJpa.findAll().filter { it.residentId == residentId }
        val deletedScenes = scenes.size
        sceneJpa.deleteAll(scenes)
        val signalsToDelete = if (bedId != null) signalJpa.findByBedId(bedId) else signalJpa.findByResidentId(residentId)
        signalJpa.deleteAll(signalsToDelete)
        var deletedProfiles = 0
        if (cleanProfiles) {
            val profiles = profileJpa.findByResidentId(residentId)
            deletedProfiles = profiles.size
            profileJpa.deleteAll(profiles)
        }
        return ResponseEntity.ok(mapOf("episodes" to deletedEpisodes, "scenes" to deletedScenes, "profiles" to deletedProfiles, "residentId" to residentId))
    }

    @DeleteMapping("/episodes")
    fun deleteEpisodes(@RequestParam residentId: String): ResponseEntity<Map<String, Int>> {
        val episodes = episodeJpa.findByResidentId(residentId)
        episodeJpa.deleteAll(episodes)
        return ResponseEntity.ok(mapOf("deleted" to episodes.size))
    }

    @GetMapping("/scene-events")
    fun listSceneEvents(
        @RequestParam residentId: String,
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?,
        @RequestParam(required = false) bedId: String? = null,
    ): ResponseEntity<List<Map<String, Any>>> {
        val all = when {
            bedId != null -> sceneJpa.findByBedId(bedId)
            else -> {
                val byResident = sceneJpa.findByResidentId(residentId)
                if (byResident.isNotEmpty()) byResident else sceneJpa.findByBedId("bed-4")
            }
        }
        val fromI = from?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() }
        val toI = to?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() }
        val filtered = all.filter { e ->
            val ts = e.timestamp
            (fromI == null || !ts.isBefore(fromI)) && (toI == null || ts.isBefore(toI.plusSeconds(1)))
        }
        return ResponseEntity.ok(filtered.map { e ->
            buildMap<String, Any> {
                put("eventType", e.eventType)
                put("at", e.timestamp.toString())
                put("bed", e.bedId)
                if (!e.residentId.isNullOrBlank()) put("residentId", e.residentId!!)
                if (!e.fromState.isNullOrBlank()) put("from", e.fromState!!)
                if (!e.toState.isNullOrBlank()) put("to", e.toState!!)
                if (!e.triggerType.isNullOrBlank()) put("trigger", e.triggerType!!)
                // V15 twinSnapshot como objeto — sin payload (no se persiste)
                if (e.twinSnapshotJson.isNotBlank() && e.twinSnapshotJson != "{}") put("twinSnapshot", parseJsonOrRaw(e.twinSnapshotJson))
                if (e.stateSince != null) put("stateSince", e.stateSince.toString())
                if (e.sceneSince != null) put("sceneSince", e.sceneSince.toString())
                val sl = e.signalLost; if (sl != null) put("signalLost", sl)
                if (!e.monitorId.isNullOrBlank()) put("monitorId", e.monitorId!!)
            }
        })
    }

    @GetMapping("/signals")
    fun listSignals(
        @RequestParam residentId: String,
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?,
        @RequestParam(required = false) bedId: String? = null,
    ): ResponseEntity<List<Map<String, Any>>> {
        val all = when {
            bedId != null -> signalJpa.findByBedId(bedId)
            else -> signalJpa.findByResidentId(residentId)
        }
        val fromI = from?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() }
        val toI = to?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() }
        val filtered = all.filter { e ->
            val ts = e.timestamp
            (fromI == null || !ts.isBefore(fromI)) && (toI == null || ts.isBefore(toI.plusSeconds(1)))
        }
        return ResponseEntity.ok(filtered.map { e ->
            buildMap<String, Any> {
                put("type", e.type)
                put("at", e.timestamp.toString())
                put("bed", e.bedId)
                if (!e.residentId.isNullOrBlank()) put("residentId", e.residentId!!)
                if (!e.episodeId.isNullOrBlank()) put("episodeId", e.episodeId!!)
                if (!e.severity.isNullOrBlank()) put("severity", e.severity!!)
                // trigger: SITTING_IN_BED para OPENED, LYING para UMBRELLA — solo si existe
                if (!e.trigger.isNullOrBlank()) put("trigger", e.trigger!!)
                if (!e.ruleId.isNullOrBlank()) put("ruleId", e.ruleId!!)
                if (!e.field.isNullOrBlank()) put("field", e.field!!)
                if (!e.triggerOn.isNullOrBlank()) put("triggerOn", e.triggerOn!!)
                if (!e.cause.isNullOrBlank()) put("cause", e.cause!!)
                if (!e.state.isNullOrBlank()) put("state", e.state!!)
                if (!e.baseline.isNullOrBlank()) put("baseline", e.baseline!!)
                if (!e.rulesFingerprint.isNullOrBlank()) put("rulesFingerprint", e.rulesFingerprint!!)
                if (!e.gapDuration.isNullOrBlank()) put("gapDuration", e.gapDuration!!)
                if (!e.previousSeverity.isNullOrBlank()) put("previousSeverity", e.previousSeverity!!)
                if (!e.originalSeverity.isNullOrBlank()) put("originalSeverity", e.originalSeverity!!)
                // V17 — detalles de regla (solo si existen, compacto por tipo) — sin payload
                if (e.reversible != null) put("reversible", e.reversible!!)
                if (e.requiresNvr != null) put("requiresNvr", e.requiresNvr!!)
                if (!e.confirmationWindow.isNullOrBlank()) put("confirmationWindow", e.confirmationWindow!!)
                if (e.requiresConfirmation != null) put("requiresConfirmation", e.requiresConfirmation!!)
                if (!e.elapsed.isNullOrBlank()) put("elapsed", e.elapsed!!)
                if (!e.threshold.isNullOrBlank()) put("threshold", e.threshold!!)
            }
        })
    }
}
