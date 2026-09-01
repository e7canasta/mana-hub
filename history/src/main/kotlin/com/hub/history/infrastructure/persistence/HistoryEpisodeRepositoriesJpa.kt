package com.hub.history.infrastructure.persistence

import com.hub.history.domain.model.*
import com.hub.history.domain.repository.HistoryEpisodeDetectionRepository
import com.hub.history.domain.repository.HistoryEpisodeInterventionRepository
import com.hub.history.domain.repository.HistoryEpisodeReviewRepository
import com.hub.shared.domain.StaffMemberId
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.BedId
import com.hub.shared.domain.BaseEntity
import com.hub.shared.time.HubClock
import jakarta.persistence.*
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Entity
@Immutable
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
    @Column(name = "activity") var activity: String? = null,
    @Column(name = "injury_status") var injuryStatus: String? = null,
    @Column(name = "self_recovery") var selfRecovery: Boolean = false,
    @Column(name = "response_seconds") var responseSeconds: Int? = null,
    @Column(name = "narrative") var narrative: String? = null,
    @Column(name = "source") var source: String = "",
    @Column(name = "model_version") var modelVersion: String? = null,
    @Column(name = "confidence") var confidence: Double? = null,
    @Column(name = "provenance_json") var provenanceJson: String = "{}",
) : BaseEntity()

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
    @Column(name = "recorded_at") var recordedAt: Instant = Instant.now(),
) : BaseEntity()

@Repository
interface HistoryEpisodeEntityRepository : JpaRepository<HistoryEpisodeEntity, String> {
    fun findBySourceRecordId(sourceRecordId: String): HistoryEpisodeEntity?
    fun findByResidentId(residentId: String): List<HistoryEpisodeEntity>
    fun findByResidentIdAndKind(residentId: String, kind: String): List<HistoryEpisodeEntity>
}

@Repository
interface HistoryEpisodeReviewEntityRepository : JpaRepository<HistoryEpisodeReviewEntity, String> {
    fun findByEpisodeId(episodeId: String): List<HistoryEpisodeReviewEntity>
}

@Repository
class HistoryEpisodeRepositoryAdapter(private val jpa: HistoryEpisodeEntityRepository, private val clock: HubClock) : HistoryEpisodeDetectionRepository {
    override fun findById(id: HistoryEpisodeId): HistoryEpisode? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findBySourceRecordId(sourceRecordId: String): HistoryEpisode? = jpa.findBySourceRecordId(sourceRecordId)?.toDomain()
    override fun findByResidentId(residentId: ResidentId): List<HistoryEpisode> = jpa.findByResidentId(residentId.value).map { it.toDomain() }
    override fun findByResidentIdAndKind(residentId: ResidentId, kind: EpisodeKind): List<HistoryEpisode> =
        jpa.findByResidentIdAndKind(residentId.value, kind.name).map { it.toDomain() }
    override fun save(detection: HistoryEpisode): HistoryEpisode = jpa.save(detection.toEntity()).toDomain()

    private fun HistoryEpisodeEntity.toDomain() = HistoryEpisode.reconstitute(
        HistoryEpisodeData(
            id = HistoryEpisodeId(id),
            sourceRecordId = sourceRecordId,
            residentId = ResidentId(residentId),
            bedId = bedId?.let { BedId(it) },
            sourceAlertId = sourceAlertId,
            kind = EpisodeKind.from(kind),
            severity = HistoryEpisodeSeverity.from(severity),
            occurredAt = occurredAt,
            activity = activity,
            injuryStatus = injuryStatus,
            selfRecovery = selfRecovery,
            responseSeconds = responseSeconds,
            narrative = narrative,
            source = EventSource.from(source),
            modelVersion = modelVersion,
            confidence = confidence,
            provenanceJson = provenanceJson,
            version = version
        )
    )

    private fun HistoryEpisode.toEntity() = HistoryEpisodeEntity(
        id.value, sourceRecordId, residentId.value, bedId?.value, sourceAlertId,
        kind.name, severity.name.lowercase(), occurredAt, activity, injuryStatus,
        selfRecovery, responseSeconds, narrative, source.name, modelVersion, confidence,
        provenanceJson
    )
}

@Repository
class HistoryEpisodeReviewRepositoryAdapter(private val jpa: HistoryEpisodeReviewEntityRepository, private val clock: HubClock) : HistoryEpisodeReviewRepository {
    override fun findByEpisodeId(episodeId: HistoryEpisodeId): List<HistoryEpisodeReview> = jpa.findByEpisodeId(episodeId.value).map { it.toDomain() }
    override fun save(review: HistoryEpisodeReview): HistoryEpisodeReview = jpa.save(review.toEntity()).toDomain()

    private fun HistoryEpisodeReviewEntity.toDomain() = HistoryEpisodeReview.reconstitute(
        HistoryEpisodeId(id), HistoryEpisodeId(episodeId), status, detectionVerdict, reviewNote, resolvedAt, actorId, version
    )
    private fun HistoryEpisodeReview.toEntity() = HistoryEpisodeReviewEntity(
        id.value, episodeId.value, status, detectionVerdict, reviewNote, resolvedAt, actorId,
        clock.now()
    )
}

@Entity
@Immutable
@Table(name = "history_episode_interventions")
class HistoryEpisodeInterventionEntity(
    @Id var id: String = "",
    @Column(name = "episode_id") var episodeId: String = "",
    @Column(name = "kind") var kind: String = "",
    @Column(name = "performed_at") var performedAt: Instant = Instant.now(),
    @Column(name = "performed_by") var performedBy: String? = null,
    @Column(name = "detail") var detail: String? = null,
) : BaseEntity()

@Repository
interface HistoryEpisodeInterventionEntityRepository : JpaRepository<HistoryEpisodeInterventionEntity, String> {
    fun findByEpisodeId(episodeId: String): List<HistoryEpisodeInterventionEntity>
    fun deleteByEpisodeId(episodeId: String)
}

@Repository
class HistoryEpisodeInterventionRepositoryAdapter(
    private val jpa: HistoryEpisodeInterventionEntityRepository
) : HistoryEpisodeInterventionRepository {

    override fun findByEpisodeId(episodeId: HistoryEpisodeId): List<HistoryEpisodeIntervention> =
        jpa.findByEpisodeId(episodeId.value).map { it.toDomain() }

    override fun save(intervention: HistoryEpisodeIntervention): HistoryEpisodeIntervention =
        jpa.save(intervention.toEntity()).toDomain()

    override fun saveAll(interventions: List<HistoryEpisodeIntervention>): List<HistoryEpisodeIntervention> =
        jpa.saveAll(interventions.map { it.toEntity() }).map { it.toDomain() }

    override fun deleteByEpisodeId(episodeId: HistoryEpisodeId) =
        jpa.deleteByEpisodeId(episodeId.value)

    private fun HistoryEpisodeInterventionEntity.toDomain() = HistoryEpisodeIntervention.reconstitute(
        id = InterventionId(id),
        episodeId = HistoryEpisodeId(episodeId),
        kind = InterventionKind.from(kind),
        performedAt = performedAt,
        performedBy = performedBy?.let { StaffMemberId(it) },
        detail = detail
    )

    private fun HistoryEpisodeIntervention.toEntity() = HistoryEpisodeInterventionEntity(
        id = id.value,
        episodeId = episodeId.value,
        kind = kind.name,
        performedAt = performedAt,
        performedBy = performedBy?.value,
        detail = detail
    )
}
