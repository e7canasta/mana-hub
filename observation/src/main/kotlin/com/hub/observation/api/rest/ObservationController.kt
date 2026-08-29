package com.hub.observation.api.rest

import com.hub.observation.application.dto.*
import com.hub.observation.application.service.BedStateService
import com.hub.observation.application.service.ObservationApplicationService
import com.hub.observation.application.service.SummaryQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1")
class ObservationController(
    private val observationApplicationService: ObservationApplicationService,
    private val summaryQueryService: SummaryQueryService,
    private val bedStateService: BedStateService
) {

    @GetMapping("/wings/{wingId}/board")
    fun getWingBoard(@PathVariable wingId: String): ResponseEntity<List<BedStateResponse>> {
        return ResponseEntity.ok(emptyList())
    }

    @GetMapping("/rooms/{roomId}/peek")
    fun peekRoom(@PathVariable roomId: String): ResponseEntity<Any> {
        return ResponseEntity.ok(mapOf("roomId" to roomId))
    }

    @GetMapping("/residents/{residentId}/sleep")
    fun getSleepSummary(
        @PathVariable residentId: String,
        @RequestParam date: LocalDate? = null,
        @RequestParam from: LocalDate? = null,
        @RequestParam to: LocalDate? = null
    ): ResponseEntity<Any> {
        return if (from != null && to != null) {
            ResponseEntity.ok(summaryQueryService.getSleepSummaryRange(residentId, from, to))
        } else if (date != null) {
            val summary = summaryQueryService.getSleepSummary(residentId, date)
            if (summary != null) ResponseEntity.ok(summary)
            else ResponseEntity.ok(mapOf("residentId" to residentId, "summaries" to emptyList<Any>()))
        } else {
            val toDefault = LocalDate.now()
            val fromDefault = toDefault.minusDays(13)
            ResponseEntity.ok(summaryQueryService.getSleepSummaryRange(residentId, fromDefault, toDefault))
        }
    }

    @GetMapping("/residents/{residentId}/mobility")
    fun getMobilitySummary(
        @PathVariable residentId: String,
        @RequestParam date: LocalDate? = null,
        @RequestParam from: LocalDate? = null,
        @RequestParam to: LocalDate? = null
    ): ResponseEntity<Any> {
        return if (from != null && to != null) {
            ResponseEntity.ok(summaryQueryService.getMobilitySummaryRange(residentId, from, to))
        } else if (date != null) {
            val summary = summaryQueryService.getMobilitySummary(residentId, date)
            if (summary != null) ResponseEntity.ok(summary)
            else ResponseEntity.ok(mapOf("residentId" to residentId, "summaries" to emptyList<Any>()))
        } else {
            val toDefault = LocalDate.now()
            val fromDefault = toDefault.minusDays(13)
            ResponseEntity.ok(summaryQueryService.getMobilitySummaryRange(residentId, fromDefault, toDefault))
        }
    }

    @GetMapping("/residents/{residentId}/bathroom")
    fun getBathroomSummary(
        @PathVariable residentId: String,
        @RequestParam date: LocalDate? = null,
        @RequestParam from: LocalDate? = null,
        @RequestParam to: LocalDate? = null
    ): ResponseEntity<Any> {
        return if (from != null && to != null) {
            ResponseEntity.ok(summaryQueryService.getBathroomSummaryRange(residentId, from, to))
        } else if (date != null) {
            val summary = summaryQueryService.getBathroomSummary(residentId, date)
            if (summary != null) ResponseEntity.ok(summary)
            else ResponseEntity.ok(mapOf("residentId" to residentId, "summaries" to emptyList<Any>()))
        } else {
            val toDefault = LocalDate.now()
            val fromDefault = toDefault.minusDays(13)
            ResponseEntity.ok(summaryQueryService.getBathroomSummaryRange(residentId, fromDefault, toDefault))
        }
    }

    @GetMapping("/residents/{residentId}/current-state")
    fun getCurrentState(@PathVariable residentId: String): ResponseEntity<CurrentStateResponse> {
        val state = bedStateService.getCurrentState(residentId)
        return ResponseEntity.ok(state)
    }

    @GetMapping("/residents/{residentId}/timeline")
    fun getTimeline(@PathVariable residentId: String): ResponseEntity<Any> {
        return ResponseEntity.ok(mapOf("residentId" to residentId))
    }

    @GetMapping("/residents/{residentId}/events")
    fun getEvents(@PathVariable residentId: String): ResponseEntity<Any> {
        return ResponseEntity.ok(mapOf("residentId" to residentId))
    }

    @GetMapping("/residents/{residentId}/notifications")
    fun getNotificationsByResident(@PathVariable residentId: String): ResponseEntity<List<NotificationResponse>> {
        return ResponseEntity.ok(observationApplicationService.getNotificationsByResident(residentId))
    }

    @GetMapping("/beds/{bedId}/notifications")
    fun getNotificationsByBed(@PathVariable bedId: String): ResponseEntity<List<NotificationResponse>> {
        return ResponseEntity.ok(observationApplicationService.getNotificationsByBed(bedId))
    }

    @GetMapping("/companion/rooms")
    fun getCompanionRooms(): ResponseEntity<Any> {
        return ResponseEntity.ok(emptyList<Any>())
    }

    @GetMapping("/reports/summary")
    fun getReportsSummary(): ResponseEntity<Any> {
        return ResponseEntity.ok(mapOf("summary" to "ok"))
    }

    @GetMapping("/catalog/states")
    fun getStateCatalog(): ResponseEntity<StateCatalogResponse> {
        return ResponseEntity.ok(StateCatalogResponse(
            states = listOf(
                StateCatalogEntry("laying_in_bed", "Acostado en cama", "bed", "person_lying"),
                StateCatalogEntry("sitting_in_bed", "Sentado en cama", "bed", "person_sitting"),
                StateCatalogEntry("sitting_on_bed_edge", "Sentado al borde", "bed", "person_sitting_edge"),
                StateCatalogEntry("standing", "De pie", "room", "person_standing"),
                StateCatalogEntry("walking", "Caminando", "room", "person_walking"),
                StateCatalogEntry("sitting_in_chair", "Sentado en silla", "room", "person_chair"),
                StateCatalogEntry("laying_on_floor", "Acostado en piso", "floor", "person_floor"),
                StateCatalogEntry("sitting_on_floor", "Sentado en piso", "floor", "person_floor"),
                StateCatalogEntry("kneeled", "Arrodillado", "floor", "person_kneeling"),
                StateCatalogEntry("absent", "Ausente", "oov", "person_absent"),
            ),
            roomStates = listOf(
                StateCatalogEntry("empty", "Vacía", "room", "room_empty"),
                StateCatalogEntry("occupied", "Ocupada", "room", "room_occupied"),
                StateCatalogEntry("unknown", "Desconocido", "room", "room_unknown"),
            )
        ))
    }
}
