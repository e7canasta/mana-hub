package com.hub.history.domain.repository

import com.hub.history.domain.model.*
import com.hub.population.domain.model.ResidentId

interface IncidentDetectionRepository {
    fun findById(id: IncidentId): IncidentDetection?
    fun findBySourceRecordId(sourceRecordId: String): IncidentDetection?
    fun findByResidentId(residentId: ResidentId): List<IncidentDetection>
    fun save(detection: IncidentDetection): IncidentDetection
}

interface IncidentReviewRepository {
    fun findByIncidentId(incidentId: IncidentId): List<IncidentReview>
    fun save(review: IncidentReview): IncidentReview
}
