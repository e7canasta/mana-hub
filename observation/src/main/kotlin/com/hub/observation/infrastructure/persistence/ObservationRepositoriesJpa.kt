package com.hub.observation.infrastructure.persistence

import com.hub.observation.domain.model.*
import com.hub.observation.domain.repository.CurrentBedStateRepository
import com.hub.observation.domain.repository.NotificationEventRepository
import com.hub.observation.domain.repository.SensorEventRepository
import com.hub.observation.domain.repository.SummaryRepository
import com.hub.population.domain.model.ResidentId
import com.hub.residence.domain.model.BedId
import com.hub.shared.domain.Identifier
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "sensor_events")
class SensorEventEntity(
    @Id var id: String = "",
    @Column(name = "source_event_id") var sourceEventId: String = "",
    @Column(name = "monitor_key") var monitorKey: String = "",
    @Column(name = "bed_id") var bedId: String? = null,
    @Column(name = "resident_id") var residentId: String? = null,
    @Column(name = "kind") var kind: String = "",
    @Column(name = "room_state") var roomState: String? = null,
    @Column(name = "substate") var substate: String? = null,
    @Column(name = "zone") var zone: String? = null,
    @Column(name = "state") var state: String? = null,
    @Column(name = "sleeping") var sleeping: Boolean? = null,
    @Column(name = "occurred_at") var occurredAt: Instant = Instant.now(),
    @Column(name = "received_at") var receivedAt: Instant = Instant.now(),
    @Column(name = "payload_json") var payloadJson: String = "{}"
)

@Entity
@Table(name = "current_bed_states")
class CurrentBedStateEntity(
    @Id var bedId: String = "",
    @Column(name = "resident_id") var residentId: String? = null,
    @Column(name = "room_state") var roomState: String? = null,
    @Column(name = "state") var state: String? = null,
    @Column(name = "substate") var substate: String? = null,
    @Column(name = "sleeping") var sleeping: Boolean? = null,
    @Column(name = "state_since") var stateSince: Instant = Instant.now(),
    @Column(name = "updated_at") var updatedAt: Instant = Instant.now(),
    @Column(name = "source") var source: String? = null,
    @Column(name = "source_event_id") var sourceEventId: String? = null
)

@Entity
@Table(name = "sleep_summaries")
class SleepSummaryEntity(
    @Id var id: String = "",
    @Column(name = "source_record_id") var sourceRecordId: String = "",
    @Column(name = "resident_id") var residentId: String = "",
    @Column(name = "observed_on") var observedOn: LocalDate = LocalDate.now(),
    @Column(name = "calm_minutes") var calmMinutes: Int = 0,
    @Column(name = "restless_minutes") var restlessMinutes: Int = 0,
    @Column(name = "awake_minutes") var awakeMinutes: Int = 0,
    @Column(name = "out_of_bed_minutes") var outOfBedMinutes: Int = 0,
    @Column(name = "bed_exit_count") var bedExitCount: Int = 0,
    @Column(name = "wake_count") var wakeCount: Int = 0,
    @Column(name = "source") var source: String? = null,
    @Column(name = "model_version") var modelVersion: String? = null,
    @Column(name = "confidence") var confidence: Double? = null,
    @Column(name = "created_at") var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at") var updatedAt: Instant = Instant.now()
)

@Entity
@Table(name = "mobility_summaries")
class MobilitySummaryEntity(
    @Id var id: String = "",
    @Column(name = "source_record_id") var sourceRecordId: String = "",
    @Column(name = "resident_id") var residentId: String = "",
    @Column(name = "observed_on") var observedOn: LocalDate = LocalDate.now(),
    @Column(name = "in_bed_minutes") var inBedMinutes: Int = 0,
    @Column(name = "out_of_bed_minutes") var outOfBedMinutes: Int = 0,
    @Column(name = "out_of_sight_minutes") var outOfSightMinutes: Int = 0,
    @Column(name = "walking_minutes") var walkingMinutes: Int = 0,
    @Column(name = "distance_meters") var distanceMeters: Double = 0.0,
    @Column(name = "transfer_count") var transferCount: Int = 0,
    @Column(name = "source") var source: String? = null,
    @Column(name = "model_version") var modelVersion: String? = null,
    @Column(name = "confidence") var confidence: Double? = null,
    @Column(name = "created_at") var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at") var updatedAt: Instant = Instant.now()
)

@Entity
@Table(name = "bathroom_summaries")
class BathroomSummaryEntity(
    @Id var id: String = "",
    @Column(name = "source_record_id") var sourceRecordId: String = "",
    @Column(name = "resident_id") var residentId: String = "",
    @Column(name = "observed_on") var observedOn: LocalDate = LocalDate.now(),
    @Column(name = "visit_count") var visitCount: Int = 0,
    @Column(name = "night_visit_count") var nightVisitCount: Int = 0,
    @Column(name = "assisted_count") var assistedCount: Int = 0,
    @Column(name = "total_minutes") var totalMinutes: Int = 0,
    @Column(name = "source") var source: String? = null,
    @Column(name = "model_version") var modelVersion: String? = null,
    @Column(name = "confidence") var confidence: Double? = null,
    @Column(name = "created_at") var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at") var updatedAt: Instant = Instant.now()
)

@Repository
interface SensorEventEntityRepository : JpaRepository<SensorEventEntity, String> {
    fun findByMonitorKey(monitorKey: String): List<SensorEventEntity>
    @Query("SELECT e FROM SensorEventEntity e WHERE e.bedId IS NULL")
    fun findUnresolved(): List<SensorEventEntity>
}

@Repository
interface CurrentBedStateEntityRepository : JpaRepository<CurrentBedStateEntity, String>

@Repository
interface SleepSummaryEntityRepository : JpaRepository<SleepSummaryEntity, String> {
    fun findByResidentIdAndObservedOn(residentId: String, observedOn: LocalDate): SleepSummaryEntity?
    fun findByResidentIdAndObservedOnBetween(residentId: String, from: LocalDate, to: LocalDate): List<SleepSummaryEntity>
}

@Repository
interface MobilitySummaryEntityRepository : JpaRepository<MobilitySummaryEntity, String> {
    fun findByResidentIdAndObservedOn(residentId: String, observedOn: LocalDate): MobilitySummaryEntity?
    fun findByResidentIdAndObservedOnBetween(residentId: String, from: LocalDate, to: LocalDate): List<MobilitySummaryEntity>
}

@Repository
interface BathroomSummaryEntityRepository : JpaRepository<BathroomSummaryEntity, String> {
    fun findByResidentIdAndObservedOn(residentId: String, observedOn: LocalDate): BathroomSummaryEntity?
    fun findByResidentIdAndObservedOnBetween(residentId: String, from: LocalDate, to: LocalDate): List<BathroomSummaryEntity>
}

@Entity
@Table(name = "notification_events")
class NotificationEventEntity(
    @Id var id: String = "",
    @Column(name = "category") var category: String = "",
    @Column(name = "bed_id") var bedId: String? = null,
    @Column(name = "resident_id") var residentId: String? = null,
    @Column(name = "event_type") var eventType: String = "",
    @Column(name = "timestamp") var timestamp: Instant = Instant.now(),
    @Column(name = "rule_id") var ruleId: String? = null,
    @Column(name = "risk_level") var riskLevel: String? = null,
    @Column(name = "payload_json") var payloadJson: String = "{}",
    @Column(name = "received_at") var receivedAt: Instant = Instant.now()
)

@Repository
interface NotificationEventEntityRepository : JpaRepository<NotificationEventEntity, String> {
    fun findByResidentId(residentId: String): List<NotificationEventEntity>
    fun findByBedId(bedId: String): List<NotificationEventEntity>
}

@Repository
class SensorEventRepositoryAdapter(private val jpa: SensorEventEntityRepository) : SensorEventRepository {
    override fun findByMonitorKey(monitorKey: String): List<SensorEvent> = jpa.findByMonitorKey(monitorKey).map { it.toDomain() }
    override fun findUnresolved(): List<SensorEvent> = jpa.findUnresolved().map { it.toDomain() }
    override fun save(event: SensorEvent): SensorEvent = jpa.save(event.toEntity()).toDomain()

    private fun SensorEventEntity.toDomain() = SensorEvent.create(
        sourceEventId, monitorKey, bedId?.let { BedId(it) }, residentId?.let { ResidentId(it) },
        kind, roomState, state, sleeping, occurredAt
    )
    private fun SensorEvent.toEntity() = SensorEventEntity(
        id.value, sourceEventId, monitorKey, bedId?.value, residentId?.value, kind,
        roomState, substate, zone, state, sleeping, occurredAt, receivedAt, payloadJson
    )
}

@Repository
class CurrentBedStateRepositoryAdapter(private val jpa: CurrentBedStateEntityRepository) : CurrentBedStateRepository {
    override fun findByBedId(bedId: BedId): CurrentBedState? = jpa.findById(bedId.value).orElse(null)?.toDomain()
    override fun findAll(): List<CurrentBedState> = jpa.findAll().map { it.toDomain() }
    override fun save(state: CurrentBedState): CurrentBedState = jpa.save(state.toEntity()).toDomain()

    private fun CurrentBedStateEntity.toDomain() = CurrentBedState(
        BedId(bedId), residentId?.let { ResidentId(it) }, roomState, state, substate,
        sleeping, stateSince, updatedAt, source, sourceEventId
    )
    private fun CurrentBedState.toEntity() = CurrentBedStateEntity(
        bedId.value, residentId?.value, roomState, state, substate, sleeping,
        stateSince, updated, source, sourceEventId
    )
}

@Repository
class SummaryRepositoryAdapter(
    private val sleepJpa: SleepSummaryEntityRepository,
    private val mobilityJpa: MobilitySummaryEntityRepository,
    private val bathroomJpa: BathroomSummaryEntityRepository
) : SummaryRepository {
    override fun findSleepByResidentAndDate(residentId: ResidentId, date: LocalDate): SleepSummary? =
        sleepJpa.findByResidentIdAndObservedOn(residentId.value, date)?.toDomain()

    override fun findSleepByResidentAndRange(residentId: ResidentId, from: LocalDate, to: LocalDate): List<SleepSummary> =
        sleepJpa.findByResidentIdAndObservedOnBetween(residentId.value, from, to).map { it.toDomain() }

    override fun findMobilityByResidentAndDate(residentId: ResidentId, date: LocalDate): MobilitySummary? =
        mobilityJpa.findByResidentIdAndObservedOn(residentId.value, date)?.toDomain()

    override fun findMobilityByResidentAndRange(residentId: ResidentId, from: LocalDate, to: LocalDate): List<MobilitySummary> =
        mobilityJpa.findByResidentIdAndObservedOnBetween(residentId.value, from, to).map { it.toDomain() }

    override fun findBathroomByResidentAndDate(residentId: ResidentId, date: LocalDate): BathroomSummary? =
        bathroomJpa.findByResidentIdAndObservedOn(residentId.value, date)?.toDomain()

    override fun findBathroomByResidentAndRange(residentId: ResidentId, from: LocalDate, to: LocalDate): List<BathroomSummary> =
        bathroomJpa.findByResidentIdAndObservedOnBetween(residentId.value, from, to).map { it.toDomain() }

    override fun saveSleep(summary: SleepSummary): SleepSummary = sleepJpa.save(summary.toEntity()).toDomain()
    override fun saveMobility(summary: MobilitySummary): MobilitySummary = mobilityJpa.save(summary.toEntity()).toDomain()
    override fun saveBathroom(summary: BathroomSummary): BathroomSummary = bathroomJpa.save(summary.toEntity()).toDomain()

    private fun SleepSummaryEntity.toDomain() = SleepSummary(
        Identifier(id), sourceRecordId, ResidentId(residentId), observedOn,
        calmMinutes, restlessMinutes, awakeMinutes, outOfBedMinutes,
        bedExitCount, wakeCount, source, modelVersion, confidence
    )
    private fun SleepSummary.toEntity() = SleepSummaryEntity(
        id.value, sourceRecordId, residentId.value, observedOn, calmMinutes,
        restlessMinutes, awakeMinutes, outOfBedMinutes, bedExitCount, wakeCount,
        source, modelVersion, confidence, Instant.now(), Instant.now()
    )

    private fun MobilitySummaryEntity.toDomain() = MobilitySummary(
        Identifier(id), sourceRecordId, ResidentId(residentId), observedOn,
        inBedMinutes, outOfBedMinutes, outOfSightMinutes, walkingMinutes,
        distanceMeters, transferCount, source, modelVersion, confidence
    )
    private fun MobilitySummary.toEntity() = MobilitySummaryEntity(
        id.value, sourceRecordId, residentId.value, observedOn, inBedMinutes,
        outOfBedMinutes, outOfSightMinutes, walkingMinutes, distanceMeters,
        transferCount, source, modelVersion, confidence, Instant.now(), Instant.now()
    )

    private fun BathroomSummaryEntity.toDomain() = BathroomSummary(
        Identifier(id), sourceRecordId, ResidentId(residentId), observedOn,
        visitCount, nightVisitCount, assistedCount, totalMinutes, source, modelVersion, confidence
    )
    private fun BathroomSummary.toEntity() = BathroomSummaryEntity(
        id.value, sourceRecordId, residentId.value, observedOn, visitCount,
        nightVisitCount, assistedCount, totalMinutes, source, modelVersion,
        confidence, Instant.now(), Instant.now()
    )
}

@Repository
class NotificationEventRepositoryAdapter(private val jpa: NotificationEventEntityRepository) : NotificationEventRepository {
    override fun findByResidentId(residentId: ResidentId): List<NotificationEvent> = jpa.findByResidentId(residentId.value).map { it.toDomain() }
    override fun findByBedId(bedId: BedId): List<NotificationEvent> = jpa.findByBedId(bedId.value).map { it.toDomain() }
    override fun save(event: NotificationEvent): NotificationEvent = jpa.save(event.toEntity()).toDomain()

    private fun NotificationEventEntity.toDomain() = NotificationEvent(
        id = Identifier(id), category = category, bedId = bedId?.let { BedId(it) },
        residentId = residentId?.let { ResidentId(it) }, eventType = eventType,
        timestamp = timestamp, ruleId = ruleId, riskLevel = riskLevel,
        payloadJson = payloadJson, receivedAt = receivedAt
    )
    private fun NotificationEvent.toEntity() = NotificationEventEntity(
        id.value, category, bedId?.value, residentId?.value, eventType,
        timestamp, ruleId, riskLevel, payloadJson, receivedAt
    )
}
