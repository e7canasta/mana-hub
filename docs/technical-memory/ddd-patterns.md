# DDD Patterns

## Aggregate Roots

Each bounded context has one or more aggregates:

| Aggregate | Root | Children |
|-----------|------|----------|
| Residence | Facility | Wing → Room → Bed |
| Population | Resident | Assignment |
| Policy | AlarmProfile | — |
| Surveillance | Episode | EpisodeNote |
| Evidence | Evidence | Timeline, ClipWindow |
| Care | Round | RoundTask, CareNote |
| History | IncidentDetection | IncidentReview |

## Bounded Contexts

See [context-map.md](../big-picture/context-map.md)

## Domain Events

Domain events are used for cross-context communication:

```kotlin
sealed interface DomainEvent {
    data class ResidentAdmitted(val residentId: String, val facilityId: String) : DomainEvent
    data class EpisodeOpened(val episodeId: String, val residentId: String) : DomainEvent
    data class EpisodeResolved(val episodeId: String, val resolution: String) : DomainEvent
}
```

## Value Objects

```kotlin
data class BedId(val value: String)
data class ResidentId(val value: String)
data class EpisodeId(val value: String)
```
