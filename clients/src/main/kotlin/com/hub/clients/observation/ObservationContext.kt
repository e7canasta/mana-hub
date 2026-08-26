package com.hub.clients.observation

import com.hub.clients.core.HttpApi
import com.hub.clients.core.ObservationDsl
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@ObservationDsl
class ObservationScope internal constructor(private val http: HttpApi) {

    // ══════════════════════════════════════════════════════════════
    //  PERCEPTION — Raw sensor reading at an instant
    //  Vocabulary: docs/vocabulario-unificado.md §3.1
    // ══════════════════════════════════════════════════════════════

    /**
     * Registers a raw perception from the sensor.
     *
     * A perception is a reading, not a decision.
     * It always passes through the Scene Engine first.
     *
     * @param monitorKey monitor/camera identifier
     * @param kind perception type (POSTURE, LOCATION, STAFF_PRESENCE, ACCESSORY_PRESENCE)
     * @param bedId bed where detected
     * @param residentId resident detected (null if staff)
     * @param state state reported by sensor
     * @param sleeping whether resident is sleeping
     */
    fun registerPerception(
        monitorKey: String,
        kind: PerceptionKind,
        bedId: String? = null,
        residentId: String? = null,
        state: String? = null,
        sleeping: Boolean? = null
    ) {
        http.postVoid(
            "/internal/v1/events",
            IngestPerceptionRequest(
                sourceEventId = UUID.randomUUID().toString(),
                monitorKey = monitorKey,
                bedId = bedId,
                residentId = residentId,
                kind = kind,
                state = state,
                sleeping = sleeping,
                occurredAt = Instant.now()
            )
        )
    }

    @Deprecated("Use registerPerception()", ReplaceWith("registerPerception(monitorKey, kind, bedId, residentId, state, sleeping)"))
    fun ingestEvent(monitorKey: String, kind: PerceptionKind, bedId: String? = null, residentId: String? = null, state: String? = null, sleeping: Boolean? = null) {
        registerPerception(monitorKey, kind, bedId, residentId, state, sleeping)
    }

    // ══════════════════════════════════════════════════════════════
    //  SCENE CHANGE — Confirmed state transition after hysteresis
    //  Vocabulary: docs/vocabulario-unificado.md §3.2
    //
    //  NOTE: Server does not have scene-change endpoints yet.
    //  This method prepares the contract. When the endpoint is
    //  implemented, it will connect automatically.
    // ══════════════════════════════════════════════════════════════

    /**
     * Registers a scene change confirmed by the scene engine.
     *
     * A scene change is the perception that survived the filter:
     * hysteresis + confidence + sensitivity.
     *
     * @param bedId bed where the change occurs
     * @param residentId affected resident
     * @param kind change type (TRANSITION, PERMANENCE)
     * @param fromState previous state
     * @param toState new state
     * @param triggerType trigger type (hysteresis, permanence, manual)
     */
    fun registerSceneChange(
        bedId: String,
        residentId: String?,
        kind: SceneChangeKind,
        fromState: String,
        toState: String,
        triggerType: TriggerType = TriggerType.HYSTERESIS
    ) {
        http.postVoid(
            "/internal/v1/scene-events",
            SceneChangeRequest(
                sourceEventId = UUID.randomUUID().toString(),
                bedId = bedId,
                residentId = residentId,
                eventType = kind,
                fromState = fromState,
                toState = toState,
                triggerType = triggerType,
                occurredAt = Instant.now()
            )
        )
    }

    /**
     * Queries scene changes for a resident.
     */
    fun sceneChanges(residentId: String): List<SceneChangeResponse> =
        http.get("/api/v1/residents/$residentId/scene-events", Array<SceneChangeResponse>::class.java).toList()

    // ══════════════════════════════════════════════════════════════
    //  CLINICAL SUMMARIES
    // ══════════════════════════════════════════════════════════════

    fun wingBoard(wingId: String): List<BedStateResponse> =
        http.get("/api/v1/wings/$wingId/board", Array<BedStateResponse>::class.java).toList()

    fun sleepSummary(residentId: String, date: LocalDate = LocalDate.now()): SleepSummaryResponse? =
        try { http.get("/api/v1/residents/$residentId/sleep?date=$date", SleepSummaryResponse::class.java) }
        catch (_: Exception) { null }

    fun mobilitySummary(residentId: String, date: LocalDate = LocalDate.now()): MobilitySummaryResponse? =
        try { http.get("/api/v1/residents/$residentId/mobility?date=$date", MobilitySummaryResponse::class.java) }
        catch (_: Exception) { null }

    fun bathroomSummary(residentId: String, date: LocalDate = LocalDate.now()): BathroomSummaryResponse? =
        try { http.get("/api/v1/residents/$residentId/bathroom?date=$date", BathroomSummaryResponse::class.java) }
        catch (_: Exception) { null }

    fun ingestSleepSummary(residentId: String, date: LocalDate, data: SleepSummaryData) {
        http.postVoid(
            "/internal/v1/clinical/sleep-summaries",
            IngestSummaryRequest(UUID.randomUUID().toString(), residentId, date, data)
        )
    }

    fun ingestMobilitySummary(residentId: String, date: LocalDate, data: MobilitySummaryData) {
        http.postVoid(
            "/internal/v1/clinical/mobility-summaries",
            IngestSummaryRequest(UUID.randomUUID().toString(), residentId, date, data)
        )
    }

    fun ingestBathroomSummary(residentId: String, date: LocalDate, data: BathroomSummaryData) {
        http.postVoid(
            "/internal/v1/clinical/bathroom-summaries",
            IngestSummaryRequest(UUID.randomUUID().toString(), residentId, date, data)
        )
    }

    fun notifyInformational(category: String, bedId: String? = null, residentId: String? = null, eventType: String, riskLevel: String? = null) {
        http.postVoid(
            "/internal/v1/notifications",
            IngestNotificationRequest(
                category = category,
                bedId = bedId,
                residentId = residentId,
                eventType = eventType,
                timestamp = Instant.now(),
                riskLevel = riskLevel
            )
        )
    }

    fun notificationsByResident(residentId: String): List<NotificationResponse> =
        http.get("/api/v1/residents/$residentId/notifications", Array<NotificationResponse>::class.java).toList()

    fun notificationsByBed(bedId: String): List<NotificationResponse> =
        http.get("/api/v1/beds/$bedId/notifications", Array<NotificationResponse>::class.java).toList()
}
