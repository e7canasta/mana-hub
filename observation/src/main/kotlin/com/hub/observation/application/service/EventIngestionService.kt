package com.hub.observation.application.service

import com.hub.observation.application.dto.IngestEventRequest
import com.hub.observation.application.dto.IngestSceneEventRequest
import com.hub.observation.application.dto.IngestSummaryRequest
import com.hub.observation.application.dto.BathroomSummaryData
import com.hub.observation.application.dto.MobilitySummaryData
import com.hub.observation.application.dto.SleepSummaryData
import com.hub.observation.domain.model.*
import com.hub.observation.domain.repository.CurrentBedStateRepository
import com.hub.observation.domain.repository.SceneEventRepository
import com.hub.observation.domain.repository.SensorEventRepository
import com.hub.observation.domain.repository.SummaryRepository
import com.hub.shared.domain.BedId
import com.hub.shared.domain.Identifier
import com.hub.shared.domain.ResidentId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class EventIngestionService(
    private val sensorEventRepository: SensorEventRepository,
    private val bedStateRepository: CurrentBedStateRepository,
    private val summaryRepository: SummaryRepository,
    private val sceneEventRepository: SceneEventRepository
) {

    @Transactional
    fun ingestEvent(request: IngestEventRequest) {
        val event = SensorEvent.create(
            sourceEventId = request.sourceEventId, monitorKey = request.monitorKey,
            bedId = request.bedId?.let { BedId(it) }, residentId = request.residentId?.let { ResidentId(it) },
            kind = SensorEventKind.from(request.kind), roomState = request.roomState, state = request.state,
            sleeping = request.sleeping, occurredAt = request.occurredAt
        )
        sensorEventRepository.save(event)

        request.bedId?.let { bedId ->
            val bedState = CurrentBedState(
                bedId = BedId(bedId), residentId = request.residentId?.let { ResidentId(it) },
                roomState = request.roomState, state = request.state, substate = null,
                sleeping = request.sleeping, stateSince = request.occurredAt, updated = Instant.now(),
                source = request.kind, sourceEventId = request.sourceEventId
            )
            bedStateRepository.save(bedState)
        }
    }

    @Transactional
    fun ingestSleepSummary(request: IngestSummaryRequest<SleepSummaryData>): Boolean {
        val rid = ResidentId(request.residentId)
        return upsert(
            find = { summaryRepository.findSleepByResidentAndDate(rid, request.observedOn) },
            create = {
                SleepSummary.create(
                    sourceRecordId = request.sourceRecordId,
                    residentId = rid,
                    observedOn = request.observedOn,
                    calmMinutes = request.data.calmMinutes,
                    restlessMinutes = request.data.restlessMinutes,
                    awakeMinutes = request.data.awakeMinutes,
                    outOfBedMinutes = request.data.outOfBedMinutes,
                    bedExitCount = request.data.bedExitCount,
                    wakeCount = request.data.wakeCount,
                    source = request.source,
                    modelVersion = request.modelVersion,
                    confidence = request.confidence,
                    startedAt = request.data.startedAt,
                    endedAt = request.data.endedAt,
                )
            },
            update = { existing ->
                existing.copy(
                    sourceRecordId = request.sourceRecordId,
                    calmMinutes = request.data.calmMinutes,
                    restlessMinutes = request.data.restlessMinutes,
                    awakeMinutes = request.data.awakeMinutes,
                    outOfBedMinutes = request.data.outOfBedMinutes,
                    bedExitCount = request.data.bedExitCount,
                    wakeCount = request.data.wakeCount,
                    source = request.source,
                    modelVersion = request.modelVersion,
                    confidence = request.confidence,
                    startedAt = request.data.startedAt,
                    endedAt = request.data.endedAt,
                )
            },
            save = { summaryRepository.saveSleep(it) },
        )
    }

    @Transactional
    fun ingestMobilitySummary(request: IngestSummaryRequest<MobilitySummaryData>): Boolean {
        val rid = ResidentId(request.residentId)
        return upsert(
            find = { summaryRepository.findMobilityByResidentAndDate(rid, request.observedOn) },
            create = {
                MobilitySummary.create(
                    sourceRecordId = request.sourceRecordId,
                    residentId = rid,
                    observedOn = request.observedOn,
                    inBedMinutes = request.data.inBedMinutes,
                    outOfBedMinutes = request.data.outOfBedMinutes,
                    outOfSightMinutes = request.data.outOfSightMinutes,
                    walkingMinutes = request.data.walkingMinutes,
                    distanceMeters = request.data.distanceMeters,
                    transferCount = request.data.transferCount,
                    source = request.source,
                    modelVersion = request.modelVersion,
                    confidence = request.confidence,
                )
            },
            update = { existing ->
                existing.copy(
                    sourceRecordId = request.sourceRecordId,
                    inBedMinutes = request.data.inBedMinutes,
                    outOfBedMinutes = request.data.outOfBedMinutes,
                    outOfSightMinutes = request.data.outOfSightMinutes,
                    walkingMinutes = request.data.walkingMinutes,
                    distanceMeters = request.data.distanceMeters,
                    transferCount = request.data.transferCount,
                    source = request.source,
                    modelVersion = request.modelVersion,
                    confidence = request.confidence,
                )
            },
            save = { summaryRepository.saveMobility(it) },
        )
    }

    @Transactional
    fun ingestBathroomSummary(request: IngestSummaryRequest<BathroomSummaryData>): Boolean {
        val rid = ResidentId(request.residentId)
        return upsert(
            find = { summaryRepository.findBathroomByResidentAndDate(rid, request.observedOn) },
            create = {
                BathroomSummary.create(
                    sourceRecordId = request.sourceRecordId,
                    residentId = rid,
                    observedOn = request.observedOn,
                    visitCount = request.data.visitCount,
                    nightVisitCount = request.data.nightVisitCount,
                    assistedCount = request.data.assistedCount,
                    totalMinutes = request.data.totalMinutes,
                    source = request.source,
                    modelVersion = request.modelVersion,
                    confidence = request.confidence,
                )
            },
            update = { existing ->
                existing.copy(
                    sourceRecordId = request.sourceRecordId,
                    visitCount = request.data.visitCount,
                    nightVisitCount = request.data.nightVisitCount,
                    assistedCount = request.data.assistedCount,
                    totalMinutes = request.data.totalMinutes,
                    source = request.source,
                    modelVersion = request.modelVersion,
                    confidence = request.confidence,
                )
            },
            save = { summaryRepository.saveBathroom(it) },
        )
    }

    private fun <T> upsert(
        find: () -> T?,
        create: () -> T,
        update: (T) -> T,
        save: (T) -> Unit,
    ): Boolean {
        val existing = find()
        val result = if (existing != null) update(existing) else create()
        save(result)
        return existing == null
    }

    @Transactional
    fun ingestSceneEvent(request: IngestSceneEventRequest) {
        val event = SceneEvent(
            id = Identifier(UUID.randomUUID().toString()),
            eventId = request.eventId ?: request.sourceEventId ?: UUID.randomUUID().toString(),
            bedId = BedId(request.bedId),
            residentId = request.residentId?.let { ResidentId(it) },
            eventType = runCatching { SceneEventType.from(request.eventType) }.getOrNull(),
            fromState = request.fromState?.let { runCatching { SceneState.from(it) }.getOrNull() },
            toState = request.toState?.let { runCatching { SceneState.from(it) }.getOrNull() },
            triggerType = request.triggerType?.let { runCatching { TriggerType.from(it) }.getOrNull() },
            timestamp = request.timestamp ?: request.occurredAt ?: Instant.now(),
            payloadJson = request.payloadJson
        )
        sceneEventRepository.save(event)
    }
}
