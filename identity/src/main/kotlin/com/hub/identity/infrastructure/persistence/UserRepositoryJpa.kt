package com.hub.identity.infrastructure.persistence

import com.hub.identity.domain.model.*
import com.hub.shared.domain.UserId
import com.hub.identity.domain.repository.UserRepository
import com.hub.shared.time.HubClock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface UserRepositoryJpa : JpaRepository<UserEntity, String> {
    fun findByUsername(username: String): UserEntity?
    fun existsByUsername(username: String): Boolean
}

@Repository
class UserRepositoryAdapter(
    private val jpa: UserRepositoryJpa,
    private val clock: HubClock
) : UserRepository {

    override fun findById(id: UserId): User? {
        return jpa.findById(id.value).orElse(null)?.toDomain()
    }

    override fun findByUsername(username: String): User? {
        return jpa.findByUsername(username)?.toDomain()
    }

    override fun save(user: User): User {
        val entity = user.toEntity()
        val saved = jpa.save(entity)
        return saved.toDomain()
    }

    override fun existsByUsername(username: String): Boolean {
        return jpa.existsByUsername(username)
    }

    override fun findAll(): List<User> {
        return jpa.findAll().map { it.toDomain() }
    }

    private fun UserEntity.toDomain(): User = User.reconstitute(
        id = UserId(id),
        username = username,
        displayName = displayName,
        role = Role.from(role),
        jobTitle = jobTitle,
        passwordHash = passwordHash,
        retiredAt = retiredAt,
        retiredBy = retiredBy?.let { UserId(it) },
        version = version
    )

    private fun User.toEntity(): UserEntity = UserEntity(
        id = id.value,
        username = username,
        displayName = displayName,
        role = role.name.lowercase(),
        jobTitle = jobTitle,
        passwordHash = passwordHash,
        retiredAt = retiredAt,
        retiredBy = retiredBy?.value,
    )
}
