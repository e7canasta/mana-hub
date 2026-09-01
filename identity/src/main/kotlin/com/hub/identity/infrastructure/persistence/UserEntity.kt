package com.hub.identity.infrastructure.persistence

import com.hub.shared.domain.BaseEntity
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @Column(name = "id")
    var id: String = "",

    @Column(name = "username", unique = true)
    var username: String = "",

    @Column(name = "display_name")
    var displayName: String = "",

    @Column(name = "role")
    var role: String = "",

    @Column(name = "job_title")
    var jobTitle: String? = null,

    @Column(name = "password_hash")
    var passwordHash: String = "",

    @Column(name = "retired_at")
    var retiredAt: Instant? = null,

    @Column(name = "retired_by")
    var retiredBy: String? = null,

    @Version var version: Long = 0,

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),
) : BaseEntity()
