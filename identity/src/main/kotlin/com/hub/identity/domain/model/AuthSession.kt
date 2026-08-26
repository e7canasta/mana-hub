package com.hub.identity.domain.model

import java.time.Instant

data class AuthSession(
    val tokenHash: ByteArray,
    val userId: UserId,
    val expiresAt: Instant,
    val createdAt: Instant = Instant.now(),
    val lastSeenAt: Instant? = null
) {
    val isExpired: Boolean get() = Instant.now().isAfter(expiresAt)

    fun touch(): AuthSession = copy(lastSeenAt = Instant.now())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AuthSession) return false
        return tokenHash.contentEquals(other.tokenHash)
    }

    override fun hashCode(): Int = tokenHash.contentHashCode()
}
