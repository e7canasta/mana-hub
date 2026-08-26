package com.hub.shared.domain.repository

import com.hub.shared.domain.AggregateRoot
import com.hub.shared.domain.Identifier

public interface Repository<T : AggregateRoot<ID>, ID : Identifier> {
    public fun findById(id: ID): T?
    public fun save(aggregate: T): T
    public fun deleteById(id: ID)
    public fun existsById(id: ID): Boolean
}

public interface ReadRepository<T, ID : Identifier> {
    public fun findById(id: ID): T?
    public fun findAll(): List<T>
    public fun existsById(id: ID): Boolean
}

public interface WriteRepository<T : AggregateRoot<ID>, ID : Identifier> {
    public fun save(aggregate: T): T
    public fun deleteById(id: ID)
}
