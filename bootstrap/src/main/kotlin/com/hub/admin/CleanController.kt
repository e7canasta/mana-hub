package com.hub.admin

import com.hub.observation.infrastructure.persistence.SceneEventEntityRepository
import com.hub.surveillance.infrastructure.persistence.EpisodeEntityRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin")
class CleanController(
    private val episodeJpa: EpisodeEntityRepository,
    private val sceneJpa: SceneEventEntityRepository,
) {
    @PostMapping("/clean")
    fun clean(@RequestParam residentId: String, @RequestParam(required = false) bedId: String? = null): ResponseEntity<Map<String, Any>> {
        val episodes = episodeJpa.findByResidentId(residentId)
        val deletedEpisodes = episodes.size
        episodeJpa.deleteAll(episodes)
        val scenes = if (bedId != null) sceneJpa.findByBedId(bedId) else sceneJpa.findAll().filter { it.residentId == residentId }
        val deletedScenes = scenes.size
        sceneJpa.deleteAll(scenes)
        return ResponseEntity.ok(mapOf("episodes" to deletedEpisodes, "scenes" to deletedScenes, "residentId" to residentId))
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
        return ResponseEntity.ok(filtered.map { mapOf("eventType" to it.eventType, "at" to it.timestamp.toString(), "bed" to it.bedId, "residentId" to (it.residentId ?: "")) })
    }
}
