package com.hub.clients.care

import com.hub.clients.core.CareDsl
import com.hub.clients.core.HttpApi
import com.hub.clients.surveillance.CreateEpisodeNoteRequest
import java.time.Instant

@CareDsl
class CareScope internal constructor(private val http: HttpApi) {

    // ══════════════════════════════════════════════════════════════
    //  ROUNDS
    // ══════════════════════════════════════════════════════════════

    fun startRound(wingId: String, scheduledFor: Instant? = null): Round {
        val resp = http.post(
            "/api/v1/rounds",
            CreateRoundRequest(wingId, scheduledFor),
            RoundResponse::class.java
        )
        return Round(http, resp)
    }

    fun currentRound(wingId: String): RoundResponse? =
        try { http.get("/api/v1/rounds/current?wingId=$wingId", RoundResponse::class.java) }
        catch (_: Exception) { null }

    fun rounds(wingId: String): List<RoundResponse> =
        http.get("/api/v1/rounds?wingId=$wingId", Array<RoundResponse>::class.java).toList()

    // ══════════════════════════════════════════════════════════════
    //  RESIDENT NOTE — All resident notes (NoteController)
    // ══════════════════════════════════════════════════════════════

    fun addResidentNote(residentId: String, authorId: String, kind: ResidentNoteType, body: String, sourceEventId: String? = null): ResidentNoteResponse =
        http.post(
            "/api/v1/residents/$residentId/notes",
            CreateResidentNoteRequest(residentId, authorId, kind, body, sourceEventId),
            ResidentNoteResponse::class.java
        )

    fun residentNotes(residentId: String): List<ResidentNoteResponse> =
        http.get("/api/v1/residents/$residentId/notes", Array<ResidentNoteResponse>::class.java).toList()

    // ══════════════════════════════════════════════════════════════
    //  FINDING — Clinical insight/conclusion (registered as resident note)
    // ══════════════════════════════════════════════════════════════

    private val findingKinds = setOf(ResidentNoteType.INSIGHT, ResidentNoteType.PATTERN, ResidentNoteType.OBSERVATION)

    fun registerFinding(residentId: String, authorId: String, findingType: ResidentNoteType, body: String, sourceEventId: String? = null): ResidentNoteResponse =
        addResidentNote(residentId, authorId, findingType, body, sourceEventId)

    fun findings(residentId: String): List<ResidentNoteResponse> =
        residentNotes(residentId).filter { it.kind in findingKinds }

    // ══════════════════════════════════════════════════════════════
    //  EPISODE NOTE — Episode notes (NoteController)
    // ══════════════════════════════════════════════════════════════

    fun addEpisodeNote(episodeId: String, authorId: String, kind: EpisodeNoteKind, body: String): EpisodeNoteResponse =
        http.post(
            "/api/v1/episodes/$episodeId/notes",
            CreateEpisodeNoteRequest(episodeId, authorId, kind, body),
            EpisodeNoteResponse::class.java
        )

    fun episodeNotes(episodeId: String): List<EpisodeNoteResponse> =
        http.get("/api/v1/episodes/$episodeId/notes", Array<EpisodeNoteResponse>::class.java).toList()

    // ══════════════════════════════════════════════════════════════
    //  SHIFT NOTE — Shift notes (NoteController)
    // ══════════════════════════════════════════════════════════════

    fun addShiftNote(facilityId: String, wingId: String?, shiftKey: String, shiftDate: String, authorId: String, kind: ShiftNoteKind, body: String): ShiftNoteResponse =
        http.post(
            "/api/v1/shift-notes",
            CreateShiftNoteRequest(facilityId, wingId, shiftKey, shiftDate, authorId, kind, body),
            ShiftNoteResponse::class.java
        )

    fun shiftNotes(facilityId: String, shiftDate: String): List<ShiftNoteResponse> =
        http.get("/api/v1/facilities/$facilityId/shift-notes?shiftDate=$shiftDate", Array<ShiftNoteResponse>::class.java).toList()

    fun wingShiftNotes(wingId: String, shiftDate: String): List<ShiftNoteResponse> =
        http.get("/api/v1/wings/$wingId/shift-notes?shiftDate=$shiftDate", Array<ShiftNoteResponse>::class.java).toList()

    // ══════════════════════════════════════════════════════════════
    //  CARE SUMMARY — daily aggregation (V7)
    // ══════════════════════════════════════════════════════════════

    fun ingestCareSummary(
        sourceRecordId: String,
        residentId: String,
        observedOn: java.time.LocalDate,
        totalMinutes: Int,
        proactiveMinutes: Int = 0,
        roundsCount: Int = 0,
        notesCount: Int = 0
    ): CareSummaryResponse =
        http.post(
            "/internal/v1/care-summaries",
            IngestCareSummaryRequest(sourceRecordId, residentId, observedOn, totalMinutes, proactiveMinutes, roundsCount, notesCount),
            CareSummaryResponse::class.java
        )

    fun careSummaries(residentId: String, from: java.time.LocalDate, to: java.time.LocalDate): CareSummaryListResponse =
        http.get("/api/v1/residents/$residentId/care?from=$from&to=$to", CareSummaryListResponse::class.java)

    fun careSummary(residentId: String): CareSummaryListResponse {
        val to = java.time.LocalDate.now()
        val from = to.minusDays(13)
        return careSummaries(residentId, from, to)
    }
}

class Round internal constructor(
    private val http: HttpApi,
    raw: RoundResponse
) {
    var raw: RoundResponse = raw
        private set

    val id: String get() = raw.id
    val wingId: String get() = raw.wingId
    val status: RoundStatus get() = raw.status

    fun complete(actorId: String): RoundResponse =
        http.patch("/api/v1/rounds/$id?actorId=$actorId", emptyMap<String, String>(), RoundResponse::class.java)

    fun completeTask(taskId: String, note: String? = null, completedBy: String? = null): RoundTaskResponse =
        http.patch("/api/v1/round-tasks/$taskId", UpdateRoundTaskRequest(note, completedBy), RoundTaskResponse::class.java)

    override fun toString(): String = "Round($wingId, $status)"
}
