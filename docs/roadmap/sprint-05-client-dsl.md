# Sprint 05: Client DSL

**Duration:** 2 weeks  
**Goal:** Kotlin client library with Client → Resource → Action → Result pattern

## Pattern

```kotlin
// Client → Resource → Action → Result
val hub = ManaHubClient("http://localhost:8080")
val session = hub.identity.login("admin", "admin123")
val facilities = hub.residence(session).facilities.list()
val facility = facilities.first()
val wings = facility.wings.list()
```

## Deliverables

### Core
- [ ] `ManaHubClient` — Entry point
- [ ] `Session` — Authenticated session
- [ ] `HttpApi` — HTTP transport

### Clients
- [ ] `IdentityClient` — Login, register users
- [ ] `ResidenceClient` — Facility, Wing, Room, Bed (nested)
- [ ] `PopulationClient` — Resident, Assignment (nested)
- [ ] `PolicyClient` — AlarmProfile, AlarmPreset
- [ ] `SurveillanceClient` — Episode, EpisodeNote (nested)
- [ ] `ObservationClient` — SensorEvent, BedState, Summaries
- [ ] `EvidenceClient` — Evidence, Timeline, ClipWindow
- [ ] `CareClient` — Round, RoundTask, Notes
- [ ] `HistoryClient` — Incident, IncidentReview
- [ ] `StreamsClient` — Stream, StreamRegion
- [ ] `AuditClient` — AuditLog

## Definition of Done

- [ ] All clients compile
- [ ] Nested resources work (facility.wings.list())
- [ ] Fluent methods return self (episode.acknowledge().resolve())
- [ ] Integration tests pass
