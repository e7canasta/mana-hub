package com.hub.history.infrastructure.persistence

import com.hub.history.domain.model.*
import com.hub.history.domain.repository.IncidentDetectionRepository
import com.hub.history.domain.repository.IncidentReviewRepository
import com.hub.population.domain.model.ResidentId
import com.hub.residence.domain.model.BedId
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Entity
@Table(name = "incident_detections")
class IncidentDetectionEntity(
    @Id var id: String = "",
    @Column(name = "source_record_id") var sourceRecordId: String = "",
    @Column(name = "resident_id") var residentId: String = "",
    @Column(name = "bed_id") var bedId: String? = null,
    @Column(name = "source_episode_id") var sourceAlertId: String? = null,
    @Column(name = "kind") var kind: String = "",
    @Column(name = "severity") var severity: String = "",
    @Column(name = "occurred_at") var occurredAt: Instant = Instant.now(),
    @Column(name = "location") var location: String? = null,
    @Column(name = "activity") var activity: String? = null,
    @Column(name = "injury_status") var injuryStatus: String? = null,
    @Column(name = "self_recovery") var selfRecovery: Boolean = false,
    @Column(name = "response_seconds") var responseSeconds: Int? = null,
    @Column(name = "narrative") var narrative: String? = null,
    @Column(name = "interventions_json") var interventionsJson: String = "[]",
    @Column(name = "source") var source: String = "",
    @Column(name = "model_version") var modelVersion: String? = null,
    @Column(name = "confidence") var confidence: Double? = null,
    @Column(name = "provenance_json") var provenanceJson: String = "{}",
    @Column(name = "created_at") var createdAt: Instant = Instant.now()
)

@Entity
@Table(name = "incident_reviews")
class IncidentReviewEntity(
    @Id var id: String = "",
    @Column(name = "incident_id") var incidentId: String = "",
    @Column(name = "status") var status: String = "",
    @Column(name = "detection_verdict") var detectionVerdict: String? = null,
    @Column(name = "review_note") var reviewNote: String? = null,
    @Column(name = "resolved_at") var resolvedAt: Instant? = null,
    @Column(name = "actor_id") var actorId: String = "",
    @Column(name = "created_at") var createdAt: Instant = Instant.now(),
    @Version var version: Long = 0
)

@Repository
interface IncidentDetectionEntityRepository : JpaRepository<IncidentDetectionEntity, String> {
    fun findBySourceRecordId(sourceRecordId: String): IncidentDetectionEntity?
    fun findByResidentId(residentId: String): List<IncidentDetectionEntity>
}

@Repository
interface IncidentReviewEntityRepository : JpaRepository<IncidentReviewEntity, String> {
    fun findByIncidentId(incidentId: String): List<IncidentReviewEntity>
}

@Repository
class IncidentDetectionRepositoryAdapter(private val jpa: IncidentDetectionEntityRepository) : IncidentDetectionRepository {
    override fun findById(id: IncidentId): IncidentDetection? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findBySourceRecordId(sourceRecordId: String): IncidentDetection? = jpa.findBySourceRecordId(sourceRecordId)?.toDomain()
    override fun findByResidentId(residentId: ResidentId): List<IncidentDetection> = jpa.findByResidentId(residentId.value).map { it.toDomain() }
    override fun save(detection: IncidentDetection): IncidentDetection = jpa.save(detection.toEntity()).toDomain()

    private fun IncidentDetectionEntity.toDomain() = IncidentDetection.reconstitute(
        IncidentId(id), sourceRecordId, ResidentId(residentId), bedId?.let { BedId(it) },
        sourceAlertId, kind, IncidentSeverity.from(severity), occurredAt, location, activity,
        injuryStatus, selfRecovery, responseSeconds, narrative, interventionsJson, source,
        modelVersion, confidence, provenanceJson, 0
    )
    private fun IncidentDetection.toEntity() = IncidentDetectionEntity(
        id.value, sourceRecordId, residentId.value, bedId?.value, sourceAlertId, kind,
        severity.name.lowercase(), occurredAt, location, activity, injuryStatus, selfRecovery,
        responseSeconds, narrative, interventionsJson, source, modelVersion, confidence,
        provenanceJson, Instant.now()
    )
}

@Repository
class IncidentReviewRepositoryAdapter(private val jpa: IncidentReviewEntityRepository) : IncidentReviewRepository {
    override fun findByIncidentId(incidentId: IncidentId): List<IncidentReview> = jpa.findByIncidentId(incidentId.value).map { it.toDomain() }
    override fun save(review: IncidentReview): IncidentReview = jpa.save(review.toEntity()).toDomain()

    private fun IncidentReviewEntity.toDomain() = IncidentReview.reconstitute(
        IncidentId(id), IncidentId(incidentId), status, detectionVerdict, reviewNote, resolvedAt, actorId, version
    )
    private fun IncidentReview.toEntity() = IncidentReviewEntity(
        id.value, incidentId.value, status, detectionVerdict, reviewNote, resolvedAt, actorId, Instant.now()
    )
}
