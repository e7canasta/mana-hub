package com.hub.identity.domain.model

import com.hub.shared.domain.AggregateRoot
import com.hub.shared.domain.UserId
import java.time.Instant

class User private constructor(
    override val id: UserId,
    val username: String,
    val displayName: String,
    val role: Role,
    val jobTitle: String?,
    val passwordHash: String,
    val retiredAt: Instant?,
    val retiredBy: UserId?,
    override var version: Long
) : AggregateRoot<UserId>() {

    val isRetired: Boolean get() = retiredAt != null

    fun retire(actorId: UserId): User {
        require(!isRetired) { "User is already retired" }
        return reconstitute(
            id = id, username = username, displayName = displayName, role = role,
            jobTitle = jobTitle, passwordHash = passwordHash,
            retiredAt = Instant.now(), retiredBy = actorId, version = version + 1
        )
    }

    fun updateProfile(displayName: String?, jobTitle: String?): User {
        return reconstitute(
            id = id, username = username, displayName = displayName ?: this.displayName,
            role = role, jobTitle = jobTitle ?: this.jobTitle, passwordHash = passwordHash,
            retiredAt = retiredAt, retiredBy = retiredBy, version = version + 1
        )
    }

    companion object {
        fun create(
            username: String,
            displayName: String,
            role: Role,
            jobTitle: String?,
            passwordHash: String
        ): User {
            val id = UserId.random()
            return User(
                id = id,
                username = username,
                displayName = displayName,
                role = role,
                jobTitle = jobTitle,
                passwordHash = passwordHash,
                retiredAt = null,
                retiredBy = null,
                version = 0
            )
        }

        fun reconstitute(
            id: UserId,
            username: String,
            displayName: String,
            role: Role,
            jobTitle: String?,
            passwordHash: String,
            retiredAt: Instant?,
            retiredBy: UserId?,
            version: Long
        ): User = User(
            id = id,
            username = username,
            displayName = displayName,
            role = role,
            jobTitle = jobTitle,
            passwordHash = passwordHash,
            retiredAt = retiredAt,
            retiredBy = retiredBy,
            version = version
        )
    }
}
