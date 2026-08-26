package com.hub.identity.infrastructure.persistence

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "auth_sessions")
class SessionEntity(
    @Id
    @Column(name = "token_hash")
    var tokenHash: ByteArray = byteArrayOf(),

    @Column(name = "user_id")
    var userId: String = "",

    @Column(name = "expires_at")
    var expiresAt: Instant = Instant.now(),

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now(),

    @Column(name = "last_seen_at")
    var lastSeenAt: Instant? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SessionEntity) return false
        return tokenHash.contentEquals(other.tokenHash)
    }

    override fun hashCode(): Int = tokenHash.contentHashCode()
}
