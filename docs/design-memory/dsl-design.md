# DSL Design

## Pattern: Client → Resource → Action → Result

Inspired by Ruby (Stripe, Twilio) and Python (boto3) API clients.

```kotlin
// Client
val hub = ManaHubClient("http://localhost:8080")

// Resource
val facilities = hub.residence(session).facilities.list()

// Action
val facility = facilities.first()

// Result
val wings = facility.wings.list()
```

## Nested Resources

```kotlin
// Facility → Wings → Rooms → Beds
facility.wings.list()
wing.rooms.list()
room.beds.list()

// Resident → Assignments
resident.assignments.list()
resident.assignTo(bed.id)

// Episode → Notes
episode.notes.list()
episode.notes.create(authorId, kind, body)
```

## Fluent Methods

```kotlin
// Methods return self for chaining
episode.acknowledge(actorId)
       .resolve("resolved")
       .addNote(actorId, "CLINICAL_NOTE", "Note body")
```

## Type Safety

```kotlin
// Enum for severity
enum class EpisodeSeverity { INFO, WARNING, CRITICAL, EMERGENCY }

// Sealed interface for states
sealed interface ResidentStatus { ACTIVE, DISCHARGED }
```
