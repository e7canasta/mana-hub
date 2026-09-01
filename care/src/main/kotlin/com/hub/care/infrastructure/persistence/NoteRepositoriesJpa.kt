package com.hub.care.infrastructure.persistence

import com.hub.care.domain.model.*
import com.hub.care.domain.repository.EpisodeNoteRepository
import com.hub.care.domain.repository.ResidentNoteRepository
import com.hub.care.domain.repository.ShiftNoteRepository
import com.hub.shared.domain.BaseEntity
import com.hub.shared.domain.EpisodeId
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.FacilityId
import com.hub.shared.domain.WingId
import com.hub.shared.domain.Identifier
import com.hub.shared.time.HubClock
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Entity
@Table(name = "resident_notes")
class ResidentNoteEntity(
    @Id var id: String = "",
    @Column(name = "resident_id") var residentId: String = "",
    @Column(name = "author_id") var authorId: String = "",
    @Column(name = "kind") var kind: String = "",
    @Column(name = "body") var body: String = "",
    @Column(name = "source_event_id") var sourceEventId: String? = null,
    @Column(name = "timestamp") var timestamp: Instant = Instant.now()
) : BaseEntity()

@Entity
@Table(name = "episode_notes")
class EpisodeNoteEntity(
    @Id var id: String = "",
    @Column(name = "episode_id") var episodeId: String = "",
    @Column(name = "author_id") var authorId: String = "",
    @Column(name = "kind") var kind: String = "",
    @Column(name = "body") var body: String = "",
    @Column(name = "timestamp") var timestamp: Instant = Instant.now()
) : BaseEntity()

@Entity
@Table(name = "shift_notes")
class ShiftNoteEntity(
    @Id var id: String = "",
    @Column(name = "facility_id") var facilityId: String = "",
    @Column(name = "wing_id") var wingId: String? = null,
    @Column(name = "shift_key") var shiftKey: String = "",
    @Column(name = "shift_date") var shiftDate: String = "",
    @Column(name = "author_id") var authorId: String = "",
    @Column(name = "kind") var kind: String = "",
    @Column(name = "body") var body: String = "",
    @Column(name = "timestamp") var timestamp: Instant = Instant.now()
) : BaseEntity()

@Repository
interface ResidentNoteEntityRepository : JpaRepository<ResidentNoteEntity, String> {
    fun findByResidentId(residentId: String): List<ResidentNoteEntity>
}

@Repository
interface EpisodeNoteEntityRepository : JpaRepository<EpisodeNoteEntity, String> {
    fun findByEpisodeId(episodeId: String): List<EpisodeNoteEntity>
}

@Repository
interface ShiftNoteEntityRepository : JpaRepository<ShiftNoteEntity, String> {
    fun findByFacilityIdAndShiftDate(facilityId: String, shiftDate: String): List<ShiftNoteEntity>
    fun findByWingIdAndShiftDate(wingId: String, shiftDate: String): List<ShiftNoteEntity>
}

@Repository
class ResidentNoteRepositoryAdapter(private val jpa: ResidentNoteEntityRepository, private val clock: HubClock) : ResidentNoteRepository {
    override fun findById(id: Identifier): ResidentNote? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findByResidentId(residentId: ResidentId): List<ResidentNote> = jpa.findByResidentId(residentId.value).map { it.toDomain() }
    override fun save(note: ResidentNote): ResidentNote = jpa.save(note.toEntity()).toDomain()

    private fun ResidentNoteEntity.toDomain() = ResidentNote(
        id = Identifier(id), residentId = ResidentId(residentId), authorId = authorId,
        kind = ResidentNoteKind.from(kind), body = body, sourceEventId = sourceEventId,
        timestamp = timestamp, createdAt = createdAt!!
    )
    private fun ResidentNote.toEntity() = ResidentNoteEntity(
        id.value, residentId.value, authorId, kind.name, body, sourceEventId, timestamp
    )
}

@Repository
class EpisodeNoteRepositoryAdapter(private val jpa: EpisodeNoteEntityRepository, private val clock: HubClock) : EpisodeNoteRepository {
    override fun findById(id: Identifier): EpisodeNote? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findByEpisodeId(episodeId: EpisodeId): List<EpisodeNote> = jpa.findByEpisodeId(episodeId.value).map { it.toDomain() }
    override fun save(note: EpisodeNote): EpisodeNote = jpa.save(note.toEntity()).toDomain()

    private fun EpisodeNoteEntity.toDomain() = EpisodeNote(
        id = Identifier(id), episodeId = EpisodeId(episodeId), authorId = authorId,
        kind = EpisodeNoteKind.from(kind), body = body, timestamp = timestamp, createdAt = createdAt!!
    )
    private fun EpisodeNote.toEntity() = EpisodeNoteEntity(
        id.value, episodeId.value, authorId, kind.name, body, timestamp
    )
}

@Repository
class ShiftNoteRepositoryAdapter(private val jpa: ShiftNoteEntityRepository, private val clock: HubClock) : ShiftNoteRepository {
    override fun findById(id: Identifier): ShiftNote? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findByFacilityAndDate(facilityId: FacilityId, shiftDate: String): List<ShiftNote> = jpa.findByFacilityIdAndShiftDate(facilityId.value, shiftDate).map { it.toDomain() }
    override fun findByWingAndDate(wingId: WingId, shiftDate: String): List<ShiftNote> = jpa.findByWingIdAndShiftDate(wingId.value, shiftDate).map { it.toDomain() }
    override fun save(note: ShiftNote): ShiftNote = jpa.save(note.toEntity()).toDomain()

    private fun ShiftNoteEntity.toDomain() = ShiftNote(
        id = Identifier(id), facilityId = FacilityId(facilityId),
        wingId = wingId?.let { WingId(it) }, shiftKey = shiftKey,
        shiftDate = shiftDate, authorId = authorId, kind = ShiftNoteKind.from(kind),
        body = body, timestamp = timestamp, createdAt = createdAt!!
    )
    private fun ShiftNote.toEntity() = ShiftNoteEntity(
        id.value, facilityId.value, wingId?.value, shiftKey, shiftDate, authorId, kind.name, body, timestamp
    )
}
