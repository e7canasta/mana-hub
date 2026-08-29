package com.hub.observation.application.service

import com.hub.observation.application.dto.IngestEventRequest
import com.hub.observation.application.dto.IngestSummaryRequest
import com.hub.observation.application.dto.BathroomSummaryData
import com.hub.observation.application.dto.MobilitySummaryData
import com.hub.observation.application.dto.SleepSummaryData
import com.hub.observation.domain.model.*
import com.hub.observation.domain.repository.CurrentBedStateRepository
import com.hub.observation.domain.repository.SensorEventRepository
import com.hub.observation.domain.repository.SummaryRepository
import com.hub.shared.domain.BedId
import com.hub.shared.domain.ResidentId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class EventIngestionService(
    private val sensorEventRepository: SensorEventRepository,
    private val bedStateRepository: CurrentBedStateRepository,
    private val summaryRepository: SummaryRepository
) {

    @Transactional
    fun ingestEvent(request: IngestEventRequest) {
        val event = SensorEvent.create(
            sourceEventId = request.sourceEventId, monitorKey = request.monitorKey,
            bedId = request.bedId?.let { BedId(it) }, residentId = request.residentId?.let { ResidentId(it) },
            kind = request.kind, roomState = request.roomState, state = request.state,
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
    fun ingestSleepSummary(request: IngestSummaryRequest<SleepSummaryData>) {
        val summary = SleepSummary.create(
            sourceRecordId = request.sourceRecordId,
            residentId = ResidentId(request.residentId),
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
            endedAt = request.data.endedAt
        )
        summaryRepository.saveSleep(summary)
    }

    @Transactional
    fun ingestMobilitySummary(request: IngestSummaryRequest<MobilitySummaryData>) {
        val summary = MobilitySummary.create(
            sourceRecordId = request.sourceRecordId,
            residentId = ResidentId(request.residentId),
            observedOn = request.observedOn,
            inBedMinutes = request.data.inBedMinutes,
            outOfBedMinutes = request.data.outOfBedMinutes,
            outOfSightMinutes = request.data.outOfSightMinutes,
            walkingMinutes = request.data.walkingMinutes,
            distanceMeters = request.data.distanceMeters,
            transferCount = request.data.transferCount,
            source = request.source,
            modelVersion = request.modelVersion,
            confidence = request.confidence
        )
        summaryRepository.saveMobility(summary)
    }

    @Transactional
    fun ingestBathroomSummary(request: IngestSummaryRequest<BathroomSummaryData>) {
        val summary = BathroomSummary.create(
            sourceRecordId = request.sourceRecordId,
            residentId = ResidentId(request.residentId),
            observedOn = request.observedOn,
            visitCount = request.data.visitCount,
            nightVisitCount = request.data.nightVisitCount,
            assistedCount = request.data.assistedCount,
            totalMinutes = request.data.totalMinutes,
            source = request.source,
            modelVersion = request.modelVersion,
            confidence = request.confidence
        )
        summaryRepository.saveBathroom(summary)
    }
}
