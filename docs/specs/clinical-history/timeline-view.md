# Spec: Timeline View (US-010)

## Context Group
**Clinical History** — History + Evidence + Care

## User Story
**As a** medical director  
**I want to** see ALL events in a resident's timeline  
**So that** I can understand their complete evolution  

## Acceptance Criteria
- [ ] Chronological timeline
- [ ] Includes: admissions, profile changes, alerts, rounds, summaries
- [ ] Each event has timestamp, type, and detail
- [ ] Can expand each event for more info
- [ ] Filter by event type

## DSL Spec (Kotlin Test)

```kotlin
class TimelineViewSpec : DescribeSpec({
    describe("US-010: View complete timeline") {
        it("should list all events for a resident") {
            // Given
            val history = hub.history(session)

            // When
            val incidents = history.incidents.list(residentId = "resident-1")

            // Then
            incidents shouldNotBeEmpty
        }
    }
})
```

## Status
📝 Pending implementation
