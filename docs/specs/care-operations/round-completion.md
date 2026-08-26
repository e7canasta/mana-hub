# Spec: Round Completion (US-013)

## Context Group
**Care Operations** — Care + Surveillance + Audit

## User Story
**As a** nurse (assigned)  
**I want to** mark tasks as completed and add notes  
**So that** the care provided is documented  

## Acceptance Criteria
- [ ] Mark each task as completed
- [ ] Add clinical note per task
- [ ] Change round status to COMPLETED
- [ ] Completion timestamp

## DSL Spec (Kotlin Test)

```kotlin
class RoundCompletionSpec : DescribeSpec({
    describe("US-013: Complete medical round") {
        it("should complete round and tasks") {
            // Given
            val round = care.rounds.create(wingId = "wing-1")

            // When
            round.complete(actorId = "nurse-1")

            // Then
            round.status shouldBe "COMPLETED"
        }
    }
})
```

## Status
📝 Pending implementation
