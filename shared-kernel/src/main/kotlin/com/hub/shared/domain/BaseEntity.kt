package com.hub.shared.domain

import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import java.time.Instant

@MappedSuperclass
open class BaseEntity {
    open var createdAt: Instant = Instant.now()

    @PrePersist
    open fun prePersist() {
        createdAt = Instant.now()
    }
}
