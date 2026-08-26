# Spec: Shift Summary

## Context Group
**Care Operations** — Care + Surveillance + Audit

## User Story
**As a** nurse  
**I want to** create a shift summary  
**So that** the next shift knows what happened  

## Acceptance Criteria
- [ ] Shift key (day, evening, night)
- [ ] Shift date
- [ ] Summary body
- [ ] Author attribution

## DSL Spec (Kotlin Test)

```kotlin
class ShiftSummarySpec : DescribeSpec({
    describe("Shift summary") {
        it("should create shift note") {
            // When
            care.shiftNotes.create(
                facilityId = "facility-1",
                wingId = null,
                shiftKey = "night",
                shiftDate = "2026-08-26",
                authorId = "nurse-1",
                kind = "SHIFT_SUMMARY",
                body = "Quiet night, no incidents"
            )

            // Then
            val notes = care.shiftNotes.list(facilityId = "facility-1", shiftDate = "2026-08-26")
            notes shouldNotBeEmpty
        }
    }
})
```

## Status
📝 Pending implementation
