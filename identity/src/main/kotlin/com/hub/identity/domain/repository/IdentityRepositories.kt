package com.hub.identity.domain.repository

import com.hub.identity.domain.model.User
import com.hub.shared.domain.UserId
import com.hub.identity.domain.model.AuthSession

interface UserRepository {
    fun findById(id: UserId): User?
    fun findAll(): List<User>
    fun findByUsername(username: String): User?
    fun save(user: User): User
    fun existsByUsername(username: String): Boolean
}

interface SessionRepository {
    fun save(session: AuthSession): AuthSession
    fun findByTokenHash(tokenHash: ByteArray): AuthSession?
    fun deleteByTokenHash(tokenHash: ByteArray)
    fun deleteExpiredSessions()
}
