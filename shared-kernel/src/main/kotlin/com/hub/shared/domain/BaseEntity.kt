package com.hub.shared.domain

import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Version
import java.time.Instant

@MappedSuperclass
abstract class BaseEntity(
    @Id open var id: String = "",
    @Version open var version: Long = 0
) {
    open var createdAt: Instant? = null
    open var updatedAt: Instant? = null

    @PrePersist
    fun prePersist() {
        if (createdAt == null) createdAt = Instant.now()
        updatedAt = Instant.now()
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = Instant.now()
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other != null && javaClass == other.javaClass && (other as BaseEntity).id == id)

    override fun hashCode(): Int = id.hashCode()
}
