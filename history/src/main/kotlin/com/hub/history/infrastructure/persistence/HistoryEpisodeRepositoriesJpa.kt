package com.hub.history.infrastructure.persistence

import com.hub.history.domain.model.*
import com.hub.history.domain.repository.HistoryEpisodeDetectionRepository
import com.hub.history.domain.repository.HistoryEpisodeReviewRepository
import com.hub.population.domain.model.ResidentId
import com.hub.residence.domain.model.BedId
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Entity
@Table(name = "history_episode_detections")
class HistoryEpisodeEntity(
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
@Table(name = "history_episode_reviews")
class HistoryEpisodeReviewEntity(
    @Id var id: String = "",
    @Column(name = "episode_id") var episodeId: String = "",
    @Column(name = "status") var status: String = "",
    @Column(name = "detection_verdict") var detectionVerdict: String? = null,
    @Column(name = "review_note") var reviewNote: String? = null,
    @Column(name = "resolved_at") var resolvedAt: Instant? = null,
    @Column(name = "actor_id") var actorId: String = "",
    @Column(name = "created_at") var createdAt: Instant = Instant.now(),
    @Version var version: Long = 0
)

@Repository
interface HistoryEpisodeEntityRepository : JpaRepository<HistoryEpisodeEntity, String> {
    fun findBySourceRecordId(sourceRecordId: String): HistoryEpisodeEntity?
    fun findByResidentId(residentId: String): List<HistoryEpisodeEntity>
}

@Repository
interface HistoryEpisodeReviewEntityRepository : JpaRepository<HistoryEpisodeReviewEntity, String> {
    fun findByEpisodeId(episodeId: String): List<HistoryEpisodeReviewEntity>
}

@Repository
class HistoryEpisodeRepositoryAdapter(private val jpa: HistoryEpisodeEntityRepository) : HistoryEpisodeDetectionRepository {
    override fun findById(id: HistoryEpisodeId): HistoryEpisode? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findBySourceRecordId(sourceRecordId: String): HistoryEpisode? = jpa.findBySourceRecordId(sourceRecordId)?.toDomain()
    override fun findByResidentId(residentId: ResidentId): List<HistoryEpisode> = jpa.findByResidentId(residentId.value).map { it.toDomain() }
    override fun save(detection: HistoryEpisode): HistoryEpisode = jpa.save(detection.toEntity()).toDomain()

    private fun HistoryEpisodeEntity.toDomain() = HistoryEpisode.reconstitute(
        HistoryEpisodeId(id), sourceRecordId, ResidentId(residentId), bedId?.let { BedId(it) },
        sourceAlertId, kind, HistoryEpisodeSeverity.from(severity), occurredAt, location, activity,
        injuryStatus, selfRecovery, responseSeconds, narrative, interventionsJson, source,
        modelVersion, confidence, provenanceJson, 0
    )
    private fun HistoryEpisode.toEntity() = HistoryEpisodeEntity(
        id.value, sourceRecordId, residentId.value, bedId?.value, sourceAlertId, kind,
        severity.name.lowercase(), occurredAt, location, activity, injuryStatus, selfRecovery,
        responseSeconds, narrative, interventionsJson, source, modelVersion, confidence,
        provenanceJson, Instant.now()
    )
}

@Repository
class HistoryEpisodeReviewRepositoryAdapter(private val jpa: HistoryEpisodeReviewEntityRepository) : HistoryEpisodeReviewRepository {
    override fun findByEpisodeId(episodeId: HistoryEpisodeId): List<HistoryEpisodeReview> = jpa.findByEpisodeId(episodeId.value).map { it.toDomain() }
    override fun save(review: HistoryEpisodeReview): HistoryEpisodeReview = jpa.save(review.toEntity()).toDomain()

    private fun HistoryEpisodeReviewEntity.toDomain() = HistoryEpisodeReview.reconstitute(
        HistoryEpisodeId(id), HistoryEpisodeId(episodeId), status, detectionVerdict, reviewNote, resolvedAt, actorId, version
    )
    private fun HistoryEpisodeReview.toEntity() = HistoryEpisodeReviewEntity(
        id.value, episodeId.value, status, detectionVerdict, reviewNote, resolvedAt, actorId, Instant.now()
    )
}
