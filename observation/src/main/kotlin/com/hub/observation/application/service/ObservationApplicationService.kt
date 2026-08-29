package com.hub.observation.application.service

import com.hub.observation.application.dto.*
import com.hub.observation.domain.model.*
import com.hub.observation.domain.repository.CurrentBedStateRepository
import com.hub.observation.domain.repository.NotificationEventRepository
import com.hub.observation.domain.repository.SensorEventRepository
import com.hub.observation.domain.repository.SummaryRepository
import com.hub.population.domain.repository.BedAssignmentRepository
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.BedId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ObservationApplicationService(
    private val sensorEventRepository: SensorEventRepository,
    private val bedStateRepository: CurrentBedStateRepository,
    private val summaryRepository: SummaryRepository,
    private val notificationEventRepository: NotificationEventRepository,
    private val bedAssignmentRepository: BedAssignmentRepository
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

    @Transactional(readOnly = true)
    fun getBedState(bedId: String): BedStateResponse? {
        return bedStateRepository.findByBedId(BedId(bedId))?.toResponse()
    }

    @Transactional(readOnly = true)
    fun getCurrentState(residentId: String): CurrentStateResponse {
        val assignment = bedAssignmentRepository.findOpenByResidentId(ResidentId(residentId))
        if (assignment == null) {
            return CurrentStateResponse(
                residentId = residentId,
                bedId = null,
                roomState = null,
                state = null,
                sleeping = null,
                stateSince = null
            )
        }
        val bedState = bedStateRepository.findByBedId(assignment.bedId)
        if (bedState == null) {
            return CurrentStateResponse(
                residentId = residentId,
                bedId = assignment.bedId.value,
                roomState = null,
                state = null,
                sleeping = null,
                stateSince = null
            )
        }
        return CurrentStateResponse(
            residentId = residentId,
            bedId = bedState.bedId.value,
            roomState = bedState.roomState,
            state = bedState.state,
            sleeping = bedState.sleeping,
            stateSince = bedState.stateSince
        )
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
            confidence = request.confidence
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

    @Transactional(readOnly = true)
    fun getSleepSummary(residentId: String, date: java.time.LocalDate): SleepSummaryResponse? {
        return summaryRepository.findSleepByResidentAndDate(ResidentId(residentId), date)?.toResponse()
    }

    @Transactional(readOnly = true)
    fun getSleepSummaryRange(residentId: String, from: java.time.LocalDate, to: java.time.LocalDate): SleepSummaryListResponse {
        val summaries = summaryRepository.findSleepByResidentAndRange(ResidentId(residentId), from, to)
            .map { it.toResponse() }
        return SleepSummaryListResponse(residentId, from, to, summaries)
    }

    @Transactional(readOnly = true)
    fun getMobilitySummary(residentId: String, date: java.time.LocalDate): MobilitySummaryResponse? {
        return summaryRepository.findMobilityByResidentAndDate(ResidentId(residentId), date)?.toResponse()
    }

    @Transactional(readOnly = true)
    fun getMobilitySummaryRange(residentId: String, from: java.time.LocalDate, to: java.time.LocalDate): MobilitySummaryListResponse {
        val summaries = summaryRepository.findMobilityByResidentAndRange(ResidentId(residentId), from, to)
            .map { it.toResponse() }
        return MobilitySummaryListResponse(residentId, from, to, summaries)
    }

    @Transactional(readOnly = true)
    fun getBathroomSummary(residentId: String, date: java.time.LocalDate): BathroomSummaryResponse? {
        return summaryRepository.findBathroomByResidentAndDate(ResidentId(residentId), date)?.toResponse()
    }

    @Transactional(readOnly = true)
    fun getBathroomSummaryRange(residentId: String, from: java.time.LocalDate, to: java.time.LocalDate): BathroomSummaryListResponse {
        val summaries = summaryRepository.findBathroomByResidentAndRange(ResidentId(residentId), from, to)
            .map { it.toResponse() }
        return BathroomSummaryListResponse(residentId, from, to, summaries)
    }

    @Transactional
    fun ingestNotification(request: IngestNotificationRequest) {
        val event = NotificationEvent.create(
            category = request.category,
            bedId = request.bedId,
            residentId = request.residentId,
            eventType = request.eventType,
            timestamp = request.timestamp,
            ruleId = request.ruleId,
            riskLevel = request.riskLevel,
            payloadJson = request.payloadJson
        )
        notificationEventRepository.save(event)
    }

    @Transactional(readOnly = true)
    fun getNotificationsByResident(residentId: String): List<NotificationResponse> {
        return notificationEventRepository.findByResidentId(ResidentId(residentId)).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getNotificationsByBed(bedId: String): List<NotificationResponse> {
        return notificationEventRepository.findByBedId(BedId(bedId)).map { it.toResponse() }
    }

    private fun NotificationEvent.toResponse() = NotificationResponse(
        id = id.value, category = category, bedId = bedId?.value, residentId = residentId?.value,
        eventType = eventType, timestamp = timestamp, ruleId = ruleId, riskLevel = riskLevel
    )

    private fun CurrentBedState.toResponse() = BedStateResponse(
        bedId = bedId.value, residentId = residentId?.value, roomState = roomState,
        state = state, sleeping = sleeping, stateSince = stateSince
    )

    private fun SleepSummary.toResponse() = SleepSummaryResponse(
        residentId = residentId.value, observedOn = observedOn, calmMinutes = calmMinutes,
        restlessMinutes = restlessMinutes, awakeMinutes = awakeMinutes, outOfBedMinutes = outOfBedMinutes,
        bedExitCount = bedExitCount, wakeCount = wakeCount
    )

    private fun MobilitySummary.toResponse() = MobilitySummaryResponse(
        residentId = residentId.value, observedOn = observedOn, walkingMinutes = walkingMinutes,
        distanceMeters = distanceMeters, transferCount = transferCount, outOfBedMinutes = outOfBedMinutes
    )

    private fun BathroomSummary.toResponse() = BathroomSummaryResponse(
        residentId = residentId.value, observedOn = observedOn, visitCount = visitCount,
        nightVisitCount = nightVisitCount
    )
}
