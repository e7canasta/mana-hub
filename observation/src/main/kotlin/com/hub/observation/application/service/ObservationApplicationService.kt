package com.hub.observation.application.service

import com.hub.observation.application.dto.*
import com.hub.observation.domain.model.NotificationCategory
import com.hub.observation.domain.model.NotificationEvent
import com.hub.observation.domain.repository.CurrentBedStateRepository
import com.hub.observation.domain.repository.NotificationEventRepository
import com.hub.shared.domain.BedId
import com.hub.shared.domain.ResidentId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ObservationApplicationService(
    private val notificationEventRepository: NotificationEventRepository,
    private val bedStateRepository: CurrentBedStateRepository
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

    private fun NotificationEvent.toResponse() = NotificationResponse(
        id = id.value, category = category.name, bedId = bedId?.value, residentId = residentId?.value,
        eventType = eventType, timestamp = timestamp, ruleId = ruleId, riskLevel = riskLevel
    )
}
