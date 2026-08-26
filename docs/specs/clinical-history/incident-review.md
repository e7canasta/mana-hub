# Spec: Incident Review

## Context Group
**Clinical History** — History + Evidence + Care

## DSL Spec (Kotlin Test)

```kotlin
class IncidentReviewSpec : DescribeSpec({
    describe("Incident review") {
        it("should review and resolve incident") {
            // Given
            val history = hub.history(session)

            // When
            val incident = history.incidents.retrieve("incident-1")
            incident.review(
                status = "RESOLVED",
                actorId = "doctor-1",
                verdict = "FALSE_POSITIVE",
                note = "Resident was stretching, not falling"
            )

            // Then
            incident.status shouldBe "RESOLVED"
        }
    }
})
```

## Status
📝 Pending implementation
