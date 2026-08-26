# ADR-001: Domain Events

## Status
Accepted

## Context
Cross-context communication needs to be decoupled.

## Decision
Use domain events for cross-context communication:

```kotlin
sealed interface DomainEvent {
    data class ResidentAdmitted(...) : DomainEvent
    data class EpisodeOpened(...) : DomainEvent
    data class EpisodeResolved(...) : DomainEvent
}
```

## Consequences
- Pro: Decoupled contexts
- Pro: Audit trail
- Con: Eventual consistency
