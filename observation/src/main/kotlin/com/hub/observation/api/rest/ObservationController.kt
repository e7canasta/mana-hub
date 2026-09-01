package com.hub.observation.api.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.hub.observation.application.dto.*
import com.hub.observation.application.service.BedStateService
import com.hub.observation.application.service.ObservationApplicationService
import com.hub.observation.application.service.SummaryQueryService
import com.hub.shared.domain.ResidentId
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDate

data class RoomPeekResponse(val roomId: String)
data class TimelineResponse(val residentId: String)
data class EventsResponse(val residentId: String)
data class CompanionRoomsResponse(val rooms: List<String>)
data class ReportsSummaryResponse(val summary: String)

@RestController
@RequestMapping("/api/v1")
class ObservationController(
    private val observationApplicationService: ObservationApplicationService,
    private val summaryQueryService: SummaryQueryService,
    private val bedStateService: BedStateService,
    private val objectMapper: ObjectMapper,
) {

    @GetMapping("/wings/{wingId}/board")
    fun getWingBoard(@PathVariable wingId: String): List<BedStateResponse> = emptyList()

    @GetMapping("/rooms/{roomId}/peek")
    fun peekRoom(@PathVariable roomId: String): RoomPeekResponse = RoomPeekResponse(roomId)

    @GetMapping("/residents/{residentId}/sleep")
    fun getSleepSummary(
        @PathVariable residentId: String,
        @RequestParam date: LocalDate? = null,
        @RequestParam from: LocalDate? = null,
        @RequestParam to: LocalDate? = null
    ): Any = if (from != null && to != null) {
        summaryQueryService.getSleepSummaryRange(residentId, from, to)
    } else if (date != null) {
        summaryQueryService.getSleepSummary(residentId, date)
            ?: SleepSummaryListResponse(residentId, date, date, emptyList())
    } else {
        val toDefault = LocalDate.now()
        val fromDefault = toDefault.minusDays(13)
        summaryQueryService.getSleepSummaryRange(residentId, fromDefault, toDefault)
    }

    @GetMapping("/residents/{residentId}/mobility")
    fun getMobilitySummary(
        @PathVariable residentId: String,
        @RequestParam date: LocalDate? = null,
        @RequestParam from: LocalDate? = null,
        @RequestParam to: LocalDate? = null
    ): Any = if (from != null && to != null) {
        summaryQueryService.getMobilitySummaryRange(residentId, from, to)
    } else if (date != null) {
        summaryQueryService.getMobilitySummary(residentId, date)
            ?: MobilitySummaryListResponse(residentId, date, date, emptyList())
    } else {
        val toDefault = LocalDate.now()
        val fromDefault = toDefault.minusDays(13)
        summaryQueryService.getMobilitySummaryRange(residentId, fromDefault, toDefault)
    }

    @GetMapping("/residents/{residentId}/bathroom")
    fun getBathroomSummary(
        @PathVariable residentId: String,
        @RequestParam date: LocalDate? = null,
        @RequestParam from: LocalDate? = null,
        @RequestParam to: LocalDate? = null
    ): Any = if (from != null && to != null) {
        summaryQueryService.getBathroomSummaryRange(residentId, from, to)
    } else if (date != null) {
        summaryQueryService.getBathroomSummary(residentId, date)
            ?: BathroomSummaryListResponse(residentId, date, date, emptyList())
    } else {
        val toDefault = LocalDate.now()
        val fromDefault = toDefault.minusDays(13)
        summaryQueryService.getBathroomSummaryRange(residentId, fromDefault, toDefault)
    }

    @GetMapping("/residents/{residentId}/current-state")
    fun getCurrentState(@PathVariable residentId: String): CurrentStateResponse =
        bedStateService.getCurrentState(residentId)

    @GetMapping("/residents/{residentId}/timeline")
    fun getTimeline(@PathVariable residentId: String): TimelineResponse = TimelineResponse(residentId)

    @GetMapping("/residents/{residentId}/events")
    fun getEvents(@PathVariable residentId: String): EventsResponse = EventsResponse(residentId)

    @GetMapping("/residents/{residentId}/scene-events")
    fun getSceneEvents(
        @PathVariable residentId: String,
        @RequestParam(required = false) from: Instant? = null,
        @RequestParam(required = false) to: Instant? = null,
    ): List<SceneEventResponse> = observationApplicationService.getSceneEvents(residentId, from, to)

    @GetMapping("/residents/{residentId}/notifications")
    fun getNotificationsByResident(@PathVariable residentId: String): List<NotificationResponse> =
        observationApplicationService.getNotificationsByResident(residentId)

    @GetMapping("/beds/{bedId}/notifications")
    fun getNotificationsByBed(@PathVariable bedId: String): List<NotificationResponse> =
        observationApplicationService.getNotificationsByBed(bedId)

    @GetMapping("/companion/rooms")
    fun getCompanionRooms(): CompanionRoomsResponse = CompanionRoomsResponse(emptyList())

    @GetMapping("/reports/summary")
    fun getReportsSummary(): ReportsSummaryResponse = ReportsSummaryResponse("ok")

    @GetMapping("/catalog/states")
    fun getStateCatalog(): StateCatalogResponse = StateCatalogResponse(
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
    )

    private fun parseTwin(json: String): Any? {
        if (json.isBlank() || json == "{}") return null
        return runCatching { objectMapper.readValue(json, Map::class.java) }.getOrNull()
    }
}
