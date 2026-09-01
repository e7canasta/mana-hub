package com.hub.shared.domain

import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version

@MappedSuperclass
abstract class BaseEntity(
    @Id open var id: String = "",
    @Version open var version: Long = 0
) {
    override fun equals(other: Any?): Boolean =
        this === other || (other != null && javaClass == other.javaClass && (other as BaseEntity).id == id)

    override fun hashCode(): Int = id.hashCode()
}
