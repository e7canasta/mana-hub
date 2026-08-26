# Spec: Resident Discharge

## Context Group
**Resident Lifecycle** — Population + Policy + Surveillance

## User Story
**As a** medical director  
**I want to** discharge a resident when they leave  
**So that** the system reflects their current status  

## Acceptance Criteria
- [ ] Status changes to DISCHARGED
- [ ] Current bed assignment is released
- [ ] Can still view historical data

## DSL Spec (Kotlin Test)

```kotlin
class ResidentDischargeSpec : DescribeSpec({
    describe("Resident discharge") {
        it("should discharge resident and release bed") {
            // Given
            val resident = ...
            val assignment = resident.assignTo(bed.id)

            // When
            resident.discharge()

            // Then
            resident.status shouldBe ResidentStatus.DISCHARGED
            resident.isDischarged shouldBe true
            val current = resident.assignments.current()
            current?.active shouldBe false
        }
    }
})
```

## Status
📝 Pending implementation
