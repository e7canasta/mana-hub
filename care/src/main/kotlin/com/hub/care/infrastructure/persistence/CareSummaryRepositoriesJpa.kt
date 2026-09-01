package com.hub.care.infrastructure.persistence

import com.hub.care.domain.model.CareSummary
import com.hub.care.domain.model.CareSummaryId
import com.hub.care.domain.repository.CareSummaryRepository
import com.hub.shared.domain.ResidentId
import com.hub.shared.time.HubClock
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "care_summaries")
class CareSummaryEntity(
    @Id var id: String = "",
    @Column(name = "source_record_id") var sourceRecordId: String = "",
    @Column(name = "resident_id") var residentId: String = "",
    @Column(name = "observed_on") var observedOn: LocalDate = LocalDate.now(),
    @Column(name = "total_minutes") var totalMinutes: Int = 0,
    @Column(name = "proactive_minutes") var proactiveMinutes: Int = 0,
    @Column(name = "rounds_count") var roundsCount: Int = 0,
    @Column(name = "notes_count") var notesCount: Int = 0,
    @Column(name = "source") var source: String? = null,
    @Column(name = "model_version") var modelVersion: String? = null,
    @Column(name = "confidence") var confidence: Double? = null,
    @Column(name = "created_at") var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at") var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0
)

@Repository
interface CareSummaryEntityRepository : JpaRepository<CareSummaryEntity, String> {
    fun findByResidentIdAndObservedOn(residentId: String, observedOn: LocalDate): CareSummaryEntity?
    fun findByResidentIdAndObservedOnBetween(residentId: String, from: LocalDate, to: LocalDate): List<CareSummaryEntity>
}

@Repository
class CareSummaryRepositoryAdapter(
    private val jpa: CareSummaryEntityRepository,
    private val clock: HubClock
) : CareSummaryRepository {

    override fun findByResidentAndDate(residentId: ResidentId, date: LocalDate): CareSummary? =
        jpa.findByResidentIdAndObservedOn(residentId.value, date)?.toDomain()

    override fun findByResidentAndRange(residentId: ResidentId, from: LocalDate, to: LocalDate): List<CareSummary> =
        jpa.findByResidentIdAndObservedOnBetween(residentId.value, from, to).map { it.toDomain() }

    override fun save(summary: CareSummary): CareSummary {
        val entity = summary.toEntity()
        jpa.findById(summary.id.value).ifPresent { old -> entity.createdAt = old.createdAt }
        entity.updatedAt = clock.now()
        return jpa.save(entity).toDomain()
    }

    private fun CareSummaryEntity.toDomain() = CareSummary.reconstitute(
        id = CareSummaryId(id),
        sourceRecordId = sourceRecordId,
        residentId = ResidentId(residentId),
        observedOn = observedOn,
        totalMinutes = totalMinutes,
        proactiveMinutes = proactiveMinutes,
        roundsCount = roundsCount,
        notesCount = notesCount,
        source = source,
        modelVersion = modelVersion,
        confidence = confidence,
        version = version
    )

    private fun CareSummary.toEntity() = CareSummaryEntity(
        id = id.value,
        sourceRecordId = sourceRecordId,
        residentId = residentId.value,
        observedOn = observedOn,
        totalMinutes = totalMinutes,
        proactiveMinutes = proactiveMinutes,
        roundsCount = roundsCount,
        notesCount = notesCount,
        source = source,
        modelVersion = modelVersion,
        confidence = confidence,
        version = version
    )
}
