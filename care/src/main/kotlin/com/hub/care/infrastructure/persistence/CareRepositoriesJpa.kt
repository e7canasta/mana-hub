package com.hub.care.infrastructure.persistence

import com.hub.care.domain.model.*
import com.hub.care.domain.repository.CareNoteRepository
import com.hub.care.domain.repository.RoundRepository
import com.hub.care.domain.repository.RoundTaskRepository
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.BedId
import com.hub.shared.domain.WingId
import jakarta.persistence.*
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Entity
@Table(name = "rounds")
class RoundEntity(
    @Id var id: String = "",
    @Column(name = "wing_id") var wingId: String = "",
    @Column(name = "status") var status: String = "in_progress",
    @Column(name = "scheduled_for") var scheduledFor: Instant? = null,
    @Column(name = "started_at") var startedAt: Instant? = null,
    @Column(name = "completed_at") var completedAt: Instant? = null,
    @Column(name = "started_by") var startedBy: String? = null,
    @Column(name = "completed_by") var completedBy: String? = null,
    @Column(name = "created_at") var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at") var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0
)

@Entity
@Table(name = "round_tasks")
class RoundTaskEntity(
    @Id var id: String = "",
    @Column(name = "round_id") var roundId: String = "",
    @Column(name = "resident_id") var residentId: String = "",
    @Column(name = "bed_id") var bedId: String? = null,
    @Column(name = "status") var status: String = "PENDING",
    @Column(name = "note") var note: String? = null,
    @Column(name = "completed_at") var completedAt: Instant? = null,
    @Column(name = "completed_by") var completedBy: String? = null,
    @Column(name = "created_at") var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at") var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0
)

@Entity
@Immutable
@Table(name = "care_notes")
class CareNoteEntity(
    @Id var id: String = "",
    @Column(name = "resident_id") var residentId: String = "",
    @Column(name = "author_id") var authorId: String = "",
    @Column(name = "kind") var kind: String = "GENERAL",
    @Column(name = "body") var body: String = "",
    @Column(name = "duration_min") var durationMin: Int? = null,
    @Column(name = "created_at") var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at") var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0
)

@Repository
interface RoundEntityRepository : JpaRepository<RoundEntity, String> {
    fun findByWingId(wingId: String): List<RoundEntity>
    @Query("SELECT e FROM RoundEntity e WHERE e.wingId = :wingId AND e.status = 'in_progress'")
    fun findInProgressByWingId(wingId: String): RoundEntity?
}

@Repository
interface RoundTaskEntityRepository : JpaRepository<RoundTaskEntity, String> {
    fun findByRoundId(roundId: String): List<RoundTaskEntity>
}

@Repository
interface CareNoteEntityRepository : JpaRepository<CareNoteEntity, String> {
    fun findByResidentId(residentId: String): List<CareNoteEntity>
}

@Repository
class RoundRepositoryAdapter(private val jpa: RoundEntityRepository) : RoundRepository {
    override fun findById(id: RoundId): Round? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findByWingId(wingId: WingId): List<Round> = jpa.findByWingId(wingId.value).map { it.toDomain() }
    override fun findInProgressByWingId(wingId: WingId): Round? = jpa.findInProgressByWingId(wingId.value)?.toDomain()
    override fun save(round: Round): Round = jpa.save(round.toEntity()).toDomain()

    private fun RoundEntity.toDomain() = Round.reconstitute(
        RoundId(id), WingId(wingId), RoundStatus.from(status), scheduledFor,
        startedAt, completedAt, startedBy, completedBy, version
    )
    private fun Round.toEntity() = RoundEntity(
        id.value, wingId.value, status.name.lowercase(), scheduledFor,
        startedAt, completedAt, startedBy, completedBy, Instant.now(), Instant.now()
    )
}

@Repository
class RoundTaskRepositoryAdapter(private val jpa: RoundTaskEntityRepository) : RoundTaskRepository {
    override fun findById(id: RoundTaskId): RoundTask? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findByRoundId(roundId: RoundId): List<RoundTask> = jpa.findByRoundId(roundId.value).map { it.toDomain() }
    override fun save(task: RoundTask): RoundTask = jpa.save(task.toEntity()).toDomain()

    private fun RoundTaskEntity.toDomain() = RoundTask.reconstitute(
        RoundTaskId(id), RoundId(roundId), ResidentId(residentId), bedId?.let { BedId(it) },
        RoundTaskStatus.from(status), note, completedAt, completedBy, version
    )
    private fun RoundTask.toEntity() = RoundTaskEntity(
        id.value, roundId.value, residentId.value, bedId?.value,
        status.name, note, completedAt, completedBy, Instant.now(), Instant.now()
    )
}

@Repository
class CareNoteRepositoryAdapter(private val jpa: CareNoteEntityRepository) : CareNoteRepository {
    override fun findByResidentId(residentId: ResidentId): List<CareNote> = jpa.findByResidentId(residentId.value).map { it.toDomain() }
    override fun save(note: CareNote): CareNote = jpa.save(note.toEntity()).toDomain()

    private fun CareNoteEntity.toDomain() = CareNote(
        CareNoteId(id), ResidentId(residentId), authorId, CareNoteKind.from(kind), body, durationMin, createdAt
    )
    private fun CareNote.toEntity() = CareNoteEntity(
        id.value, residentId.value, authorId, kind.name, body, durationMin, createdAt, Instant.now()
    )
}
