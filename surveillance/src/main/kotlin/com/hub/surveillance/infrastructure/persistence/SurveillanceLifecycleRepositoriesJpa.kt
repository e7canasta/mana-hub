package com.hub.surveillance.infrastructure.persistence

import com.hub.shared.domain.BaseEntity
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Entity
@Table(name = "episode_transitions")
class EpisodeTransitionEntity(
    @Id var id: String = "",
    @Column(name = "episode_id") var episodeId: String = "",
    @Column(name = "from_status") var fromStatus: String? = null,
    @Column(name = "to_status") var toStatus: String = "",
    @Column(name = "actor_id") var actorId: String = "",
    @Column(name = "occurred_at") var occurredAt: Instant = Instant.now(),
    @Column(name = "sequence") var sequence: Int = 0
)

@Entity
@Table(name = "notification_deliveries")
class NotificationDeliveryEntity(
    @Id var id: String = "",
    @Column(name = "episode_id") var episodeId: String = "",
    @Column(name = "recipient_kind") var recipientKind: String = "",
    @Column(name = "recipient_id") var recipientId: String = "",
    @Column(name = "channel") var channel: String = "",
    @Column(name = "escalation_level") var escalationLevel: Int = 0,
) : BaseEntity()

@Entity
@Table(name = "notification_delivery_events")
class NotificationDeliveryEventEntity(
    @Id var id: String = "",
    @Column(name = "delivery_id") var deliveryId: String = "",
    @Column(name = "kind") var kind: String = "",
    @Column(name = "reason") var reason: String? = null,
    @Column(name = "occurred_at") var occurredAt: Instant = Instant.now()
)

@Entity
@Table(name = "episode_escalations")
class EpisodeEscalationEntity(
    @Id var id: String = "",
    @Column(name = "episode_id") var episodeId: String = "",
    @Column(name = "level") var level: Int = 0,
    @Column(name = "target_id") var targetId: String = "",
    @Column(name = "occurred_at") var occurredAt: Instant = Instant.now(),
) : BaseEntity()

@Repository
interface EpisodeTransitionEntityRepository : JpaRepository<EpisodeTransitionEntity, String> {
    fun findByEpisodeIdOrderBySequenceAsc(episodeId: String): List<EpisodeTransitionEntity>
}

@Repository
interface NotificationDeliveryEntityRepository : JpaRepository<NotificationDeliveryEntity, String> {
    fun findByEpisodeId(episodeId: String): List<NotificationDeliveryEntity>
}

@Repository
interface NotificationDeliveryEventEntityRepository : JpaRepository<NotificationDeliveryEventEntity, String> {
    fun findByDeliveryId(deliveryId: String): List<NotificationDeliveryEventEntity>
}

@Repository
interface EpisodeEscalationEntityRepository : JpaRepository<EpisodeEscalationEntity, String> {
    fun findByEpisodeId(episodeId: String): List<EpisodeEscalationEntity>
}
