# Spec: Clinical Summary (US-011)

## Context Group
**Clinical History** — History + Evidence + Care

## User Story
**As a** medical director  
**I want to** see daily sleep, mobility, and bathroom summaries  
**So that** I can detect trends and anomalies  

## Acceptance Criteria
- [ ] Sleep summary: hours, awakenings, time out of bed
- [ ] Mobility summary: distance, transfers
- [ ] Bathroom summary: visits, nighttime
- [ ] Compare with previous days

## DSL Spec (Kotlin Test)

```kotlin
class ClinicalSummarySpec : DescribeSpec({
    describe("US-011: View clinical summaries") {
        it("should retrieve sleep summary") {
            // Given
            val observation = hub.observation(session)

            // When
            val sleep = observation.sleepSummary(residentId = "resident-1", date = "2026-08-25")

            // Then
            sleep shouldNotBeNull {
                it.data.calmMinutes should beGreaterThan(0)
            }
        }
    }
})
```

## Status
📝 Pending implementation
