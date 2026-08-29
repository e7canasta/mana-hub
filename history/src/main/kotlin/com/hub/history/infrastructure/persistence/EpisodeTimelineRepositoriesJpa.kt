package com.hub.history.infrastructure.persistence

import com.hub.history.domain.model.timeline.EpisodeTimelineEvent
import com.hub.history.domain.model.timeline.EpisodeTimelineEventId
import com.hub.history.domain.model.timeline.EpisodeTimelineRepository
import com.hub.history.domain.model.timeline.EventType
import com.hub.shared.domain.ResidentId
import jakarta.persistence.*
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Entity
@Table(name = "episode_timeline_events")
class EpisodeTimelineEventEntity(
    @Id var id: String = "",
    @Column(name = "episode_id") var episodeId: String = "",
    @Column(name = "resident_id") var residentId: String = "",
    @Column(name = "at") var at: Instant = Instant.now(),
    @Column(name = "type") var type: String = "",
    @Column(name = "from_state") var fromState: String? = null,
    @Column(name = "to_state") var toState: String? = null,
    @Column(name = "description") var description: String? = null,
    @Column(name = "created_at") var createdAt: Instant = Instant.now(),
)

@Repository
interface EpisodeTimelineEventEntityRepository : JpaRepository<EpisodeTimelineEventEntity, String> {
    fun findByEpisodeIdOrderByAtAsc(episodeId: String, page: PageRequest): List<EpisodeTimelineEventEntity>
    fun findByResidentIdOrderByAtDesc(residentId: String, page: PageRequest): List<EpisodeTimelineEventEntity>
}

@Repository
class EpisodeTimelineRepositoryAdapter(
    private val jpa: EpisodeTimelineEventEntityRepository,
) : EpisodeTimelineRepository {

    override fun findByEpisodeId(episodeId: String, offset: Int, limit: Int): List<EpisodeTimelineEvent> =
        jpa.findByEpisodeIdOrderByAtAsc(episodeId, PageRequest.of(offset / limit, limit)).map { it.toDomain() }

    override fun findByResidentId(residentId: ResidentId, offset: Int, limit: Int): List<EpisodeTimelineEvent> =
        jpa.findByResidentIdOrderByAtDesc(residentId.value, PageRequest.of(offset / limit, limit)).map { it.toDomain() }

    override fun save(event: EpisodeTimelineEvent): EpisodeTimelineEvent =
        jpa.save(event.toEntity()).toDomain()

    private fun EpisodeTimelineEventEntity.toDomain() = EpisodeTimelineEvent(
        id = EpisodeTimelineEventId.from(id),
        episodeId = episodeId,
        residentId = ResidentId(residentId),
        at = at,
        type = EventType.valueOf(type),
        fromState = fromState,
        toState = toState,
        description = description,
    )

    private fun EpisodeTimelineEvent.toEntity() = EpisodeTimelineEventEntity(
        id = id.value,
        episodeId = episodeId,
        residentId = residentId.value,
        at = at,
        type = type.name,
        fromState = fromState,
        toState = toState,
        description = description,
    )
}
