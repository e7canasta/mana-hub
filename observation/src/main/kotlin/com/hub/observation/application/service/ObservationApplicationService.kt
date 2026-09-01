package com.hub.observation.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.hub.observation.application.dto.*
import com.hub.observation.domain.model.NotificationCategory
import com.hub.observation.domain.model.NotificationEvent
import com.hub.observation.domain.repository.CurrentBedStateRepository
import com.hub.observation.domain.repository.NotificationEventRepository
import com.hub.observation.domain.repository.SceneEventRepository
import com.hub.shared.domain.BedId
import com.hub.shared.domain.ResidentId
import com.manahive.contracts.scene.SceneEvent as HiveSceneEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ObservationApplicationService(
    private val notificationEventRepository: NotificationEventRepository,
    private val bedStateRepository: CurrentBedStateRepository,
    private val sceneEventRepository: SceneEventRepository,
    private val objectMapper: ObjectMapper,
) {

    @Transactional
    fun ingestNotification(request: IngestNotificationRequest) {
        val event = NotificationEvent.create(
            category = NotificationCategory.from(request.category),
            bedId = request.bedId,
            residentId = request.residentId,
            eventType = request.eventType,
            timestamp = request.timestamp,
            ruleId = request.ruleId,
            riskLevel = request.riskLevel,
            payloadJson = request.payloadJson
        )
        notificationEventRepository.save(event)

        if (request.bedId != null && request.eventType in listOf("staff_entered", "staff_exited")) {
            bedStateRepository.updateStaffPresent(BedId(request.bedId), request.eventType == "staff_entered")
        }
    }

    @Transactional(readOnly = true)
    fun getNotificationsByResident(residentId: String): List<NotificationResponse> {
        return notificationEventRepository.findByResidentId(ResidentId(residentId)).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getNotificationsByBed(bedId: String): List<NotificationResponse> {
        return notificationEventRepository.findByBedId(BedId(bedId)).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getSceneEvents(residentId: String, from: Instant?, to: Instant?): List<SceneEventResponse> {
        val rid = ResidentId(residentId)
        val events = if (from != null && to != null)
            sceneEventRepository.findByResidentId(rid, from, to)
        else
            sceneEventRepository.findByResidentId(rid)
        val nightOpened = HiveSceneEvent.NightOpened::class.simpleName
        return events.map {
            SceneEventResponse(
                id = it.id.value,
                eventId = it.eventId,
                bedId = it.bedId.value,
                residentId = it.residentId?.value,
                eventType = it.eventType?.name ?: "UNKNOWN",
                fromState = it.fromState?.name,
                toState = it.toState?.name,
                triggerType = it.triggerType?.name,
                timestamp = it.timestamp,
                type = it.eventType?.name ?: "UNKNOWN",
                at = it.timestamp,
                from = it.fromState?.name,
                to = it.toState?.name,
                initialState = if (it.eventType?.name == nightOpened) it.toState?.name else null,
                twinSnapshot = runCatching {
                    if (it.twinSnapshotJson.isBlank() || it.twinSnapshotJson == "{}") null
                    else objectMapper.readValue(it.twinSnapshotJson, Map::class.java)
                }.getOrNull(),
            )
        }
    }

    private fun NotificationEvent.toResponse() = NotificationResponse(
        id = id.value, category = category.name, bedId = bedId?.value, residentId = residentId?.value,
        eventType = eventType, timestamp = timestamp, ruleId = ruleId, riskLevel = riskLevel
    )
}
