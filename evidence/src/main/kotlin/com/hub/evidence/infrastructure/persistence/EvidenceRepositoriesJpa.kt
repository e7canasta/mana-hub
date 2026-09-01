package com.hub.evidence.infrastructure.persistence

import com.hub.evidence.domain.model.*
import com.hub.evidence.domain.repository.ClipWindowRepository
import com.hub.evidence.domain.repository.EvidenceRepository
import com.hub.evidence.domain.repository.TimelineRepository
import com.hub.shared.domain.BaseEntity
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.BedId
import com.hub.shared.time.HubClock
import jakarta.persistence.*
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Entity
@Immutable
@Table(name = "evidence")
class EvidenceEntity(
    @Id var id: String = "",
    @Column(name = "bed_id") var bedId: String = "",
    @Column(name = "resident_id") var residentId: String = "",
    @Column(name = "evidence_type") var evidenceType: String = "",
    @Column(name = "category") var category: String? = null,
    @Column(name = "scene_event_id") var sceneEventId: String? = null,
    @Column(name = "scene_event_json") var sceneEventJson: String? = null,
    @Column(name = "rule_id") var ruleId: String? = null,
    @Column(name = "shift") var shift: String? = null,
    @Column(name = "risk_level") var riskLevel: String? = null,
    @Column(name = "timestamp") var timestamp: Instant = Instant.now(),
    @Version var version: Long = 0,
) : BaseEntity()

@Entity
@Table(name = "timelines")
class TimelineEntity(
    @Id var id: String = "",
    @Column(name = "bed_id") var bedId: String = "",
    @Column(name = "resident_id") var residentId: String = "",
    @Column(name = "anchor_event_id") var anchorEventId: String? = null,
    @Column(name = "anchor_event_json") var anchorEventJson: String? = null,
    @Column(name = "before_events_json") var beforeEventsJson: String = "[]",
    @Column(name = "after_events_json") var afterEventsJson: String = "[]",
    @Column(name = "window_start") var windowStart: Instant = Instant.now(),
    @Column(name = "window_end") var windowEnd: Instant? = null,
    @Column(name = "closed_at") var closedAt: Instant? = null,
    @Version var version: Long = 0,
) : BaseEntity()

@Entity
@Table(name = "clip_windows")
class ClipWindowEntity(
    @Id @Column(name = "window_id") var id: String = "",
    @Column(name = "bed_id") var bedId: String = "",
    @Column(name = "resident_id") var residentId: String = "",
    @Column(name = "started_at") var startedAt: Instant = Instant.now(),
    @Column(name = "ended_at") var endedAt: Instant? = null,
    @Column(name = "timeout_minutes") var timeoutMinutes: Int = 5,
    @Column(name = "events_json") var eventsJson: String = "[]",
    @Column(name = "state") var state: String = "open",
    @Column(name = "close_condition_json") var closeConditionJson: String? = null,
    @Column(name = "closed_at") var closedAt: Instant? = null,
    @Version var version: Long = 0,
) : BaseEntity()

@Repository
interface EvidenceEntityRepository : JpaRepository<EvidenceEntity, String> {
    fun findByBedId(bedId: String): List<EvidenceEntity>
}

@Repository
interface TimelineEntityRepository : JpaRepository<TimelineEntity, String>

@Repository
interface ClipWindowEntityRepository : JpaRepository<ClipWindowEntity, String> {
    @Query("SELECT e FROM ClipWindowEntity e WHERE e.bedId = :bedId AND e.state = 'open'")
    fun findOpenByBedId(bedId: String): ClipWindowEntity?
}

@Repository
class EvidenceRepositoryAdapter(private val jpa: EvidenceEntityRepository, private val clock: HubClock) : EvidenceRepository {
    override fun findById(id: EvidenceId): Evidence? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findByBedId(bedId: BedId): List<Evidence> = jpa.findByBedId(bedId.value).map { it.toDomain() }
    override fun save(evidence: Evidence): Evidence = jpa.save(evidence.toEntity()).toDomain()

    private fun EvidenceEntity.toDomain() = Evidence.reconstitute(
        EvidenceId(id), BedId(bedId), ResidentId(residentId), evidenceType, category,
        sceneEventId, sceneEventJson, ruleId, shift, riskLevel, timestamp, version
    )
    private fun Evidence.toEntity() = EvidenceEntity(
        id.value, bedId.value, residentId.value, evidenceType, category, sceneEventId,
        sceneEventJson, ruleId, shift, riskLevel, timestamp
    )
}

@Repository
class TimelineRepositoryAdapter(private val jpa: TimelineEntityRepository, private val clock: HubClock) : TimelineRepository {
    override fun findById(id: EvidenceId): Timeline? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun save(timeline: Timeline): Timeline = jpa.save(timeline.toEntity()).toDomain()

    private fun TimelineEntity.toDomain() = Timeline.reconstitute(
        EvidenceId(id), BedId(bedId), ResidentId(residentId), anchorEventId, anchorEventJson,
        beforeEventsJson, afterEventsJson, windowStart, windowEnd, closedAt, version
    )
    private fun Timeline.toEntity() = TimelineEntity(
        id.value, bedId.value, residentId.value, anchorEventId, anchorEventJson,
        beforeEventsJson, afterEventsJson, windowStart, windowEnd, closedAt
    )
}

@Repository
class ClipWindowRepositoryAdapter(private val jpa: ClipWindowEntityRepository, private val clock: HubClock) : ClipWindowRepository {
    override fun findById(id: EvidenceId): ClipWindow? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findOpenByBedId(bedId: BedId): ClipWindow? = jpa.findOpenByBedId(bedId.value)?.toDomain()
    override fun save(clipWindow: ClipWindow): ClipWindow = jpa.save(clipWindow.toEntity()).toDomain()

    private fun ClipWindowEntity.toDomain() = ClipWindow.reconstitute(
        EvidenceId(id), BedId(bedId), ResidentId(residentId), startedAt, endedAt,
        timeoutMinutes, eventsJson, state, closeConditionJson, closedAt, version
    )
    private fun ClipWindow.toEntity() = ClipWindowEntity(
        id.value, bedId.value, residentId.value, startedAt, endedAt, timeoutMinutes,
        eventsJson, state, closeConditionJson, closedAt
    )
}
