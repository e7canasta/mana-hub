# Spec: Round Creation (US-012)

## Context Group
**Care Operations** — Care + Surveillance + Audit

## User Story
**As a** medical director  
**I want to** create a medical round for one or more residents  
**So that** necessary reviews are performed  

## Acceptance Criteria
- [ ] Select residents
- [ ] Define tasks (blood pressure, weight, etc.)
- [ ] Assign to nurse
- [ ] Scheduled date/time
- [ ] Status: PENDING

## DSL Spec (Kotlin Test)

```kotlin
class RoundCreationSpec : DescribeSpec({
    describe("US-012: Create medical round") {
        it("should create round with tasks") {
            // Given
            val care = hub.care(session)

            // When
            val round = care.rounds.create(wingId = "wing-1") {
                scheduledFor = Instant.now().plus(1, ChronoUnit.HOURS)
            }

            // Then
            round.wingId shouldBe "wing-1"
            round.status shouldBe "PENDING"
        }
    }
})
```

## Status
📝 Pending implementation
