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
import java.time.Instant

@RestController
@RequestMapping("/api/v1/admin")
class CleanController(
    private val episodeJpa: EpisodeEntityRepository,
    private val sceneJpa: SceneEventEntityRepository,
    private val signalJpa: SentinelSignalEntityRepository,
    private val profileJpa: ResidentProfileEntityRepository,
) {
    private val mapper = ObjectMapper().apply { registerModule(JavaTimeModule()) }

    @PostMapping("/clean")
    fun clean(
        @RequestParam residentId: String,
        @RequestParam(required = false) bedId: String? = null,
        @RequestParam(required = false, defaultValue = "false") cleanProfiles: Boolean = false,
    ): ResponseEntity<Map<String, Any>> {
        val episodes = episodeJpa.findByResidentId(residentId)
        episodeJpa.deleteAll(episodes)
        val scenes = if (bedId != null) sceneJpa.findByBedId(bedId)
        else sceneJpa.findAll().filter { it.residentId == residentId }
        sceneJpa.deleteAll(scenes)
        val signals = if (bedId != null) signalJpa.findByBedId(bedId)
        else signalJpa.findByResidentId(residentId)
        signalJpa.deleteAll(signals)
        var deletedProfiles = 0
        if (cleanProfiles) {
            val profiles = profileJpa.findByResidentId(residentId)
            deletedProfiles = profiles.size
            profileJpa.deleteAll(profiles)
        }
        return ResponseEntity.ok(mapOf(
            "episodes" to episodes.size,
            "scenes" to scenes.size,
            "profiles" to deletedProfiles,
            "residentId" to residentId,
        ))
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
        val all = fetchSceneEvents(residentId, bedId)
        val range = TimeRange.parse(from, to)
        return ResponseEntity.ok(all.filter { range.contains(it.timestamp) }.map { it.toMap() })
    }

    @GetMapping("/signals")
    fun listSignals(
        @RequestParam residentId: String,
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?,
        @RequestParam(required = false) bedId: String? = null,
    ): ResponseEntity<List<Map<String, Any>>> {
        val all = fetchSignals(residentId, bedId)
        val range = TimeRange.parse(from, to)
        return ResponseEntity.ok(all.filter { range.contains(it.timestamp) }.map { it.toMap() })
    }

    private fun fetchSceneEvents(residentId: String, bedId: String?) = when {
        bedId != null -> sceneJpa.findByBedId(bedId)
        else -> sceneJpa.findByResidentId(residentId).ifEmpty { sceneJpa.findByBedId("bed-4") }
    }

    private fun fetchSignals(residentId: String, bedId: String?) = when {
        bedId != null -> signalJpa.findByBedId(bedId)
        else -> signalJpa.findByResidentId(residentId)
    }

    private fun parseJsonOrRaw(json: String): Any = try {
        mapper.readValue(json, object : TypeReference<Map<String, Any>>() {})
    } catch (_: Exception) { json }

    private data class TimeRange(val from: Instant?, val to: Instant?) {
        fun contains(ts: Instant): Boolean =
            (from == null || !ts.isBefore(from)) && (to == null || ts.isBefore(to.plusSeconds(1)))

        companion object {
            fun parse(from: String?, to: String?) = TimeRange(
                from = from?.let { runCatching { Instant.parse(it) }.getOrNull() },
                to = to?.let { runCatching { Instant.parse(it) }.getOrNull() },
            )
        }
    }
}

private fun com.hub.observation.infrastructure.persistence.SceneEventEntity.toMap(): Map<String, Any> = buildMap {
    put("eventType", eventType)
    put("at", timestamp.toString())
    put("bed", bedId)
    residentId?.takeIf { it.isNotBlank() }?.let { put("residentId", it) }
    fromState?.takeIf { it.isNotBlank() }?.let { put("from", it) }
    toState?.takeIf { it.isNotBlank() }?.let { put("to", it) }
    triggerType?.takeIf { it.isNotBlank() }?.let { put("trigger", it) }
    if (twinSnapshotJson.isNotBlank() && twinSnapshotJson != "{}") put("twinSnapshot", twinSnapshotJson)
    stateSince?.let { put("stateSince", it.toString()) }
    sceneSince?.let { put("sceneSince", it.toString()) }
    signalLost?.let { put("signalLost", it) }
    monitorId?.takeIf { it.isNotBlank() }?.let { put("monitorId", it) }
}

private fun com.hub.observation.infrastructure.persistence.SentinelSignalEntity.toMap(): Map<String, Any> = buildMap {
    put("type", type)
    put("at", timestamp.toString())
    put("bed", bedId)
    residentId?.takeIf { it.isNotBlank() }?.let { put("residentId", it) }
    episodeId?.takeIf { it.isNotBlank() }?.let { put("episodeId", it) }
    severity?.takeIf { it.isNotBlank() }?.let { put("severity", it) }
    trigger?.takeIf { it.isNotBlank() }?.let { put("trigger", it) }
    ruleId?.takeIf { it.isNotBlank() }?.let { put("ruleId", it) }
    field?.takeIf { it.isNotBlank() }?.let { put("field", it) }
    triggerOn?.takeIf { it.isNotBlank() }?.let { put("triggerOn", it) }
    cause?.takeIf { it.isNotBlank() }?.let { put("cause", it) }
    state?.takeIf { it.isNotBlank() }?.let { put("state", it) }
    baseline?.takeIf { it.isNotBlank() }?.let { put("baseline", it) }
    rulesFingerprint?.takeIf { it.isNotBlank() }?.let { put("rulesFingerprint", it) }
    gapDuration?.takeIf { it.isNotBlank() }?.let { put("gapDuration", it) }
    previousSeverity?.takeIf { it.isNotBlank() }?.let { put("previousSeverity", it) }
    originalSeverity?.takeIf { it.isNotBlank() }?.let { put("originalSeverity", it) }
    reversible?.let { put("reversible", it) }
    requiresNvr?.let { put("requiresNvr", it) }
    confirmationWindow?.takeIf { it.isNotBlank() }?.let { put("confirmationWindow", it) }
    requiresConfirmation?.let { put("requiresConfirmation", it) }
    elapsed?.takeIf { it.isNotBlank() }?.let { put("elapsed", it) }
    threshold?.takeIf { it.isNotBlank() }?.let { put("threshold", it) }
}
