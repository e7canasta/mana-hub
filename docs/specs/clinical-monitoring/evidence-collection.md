# Spec: Evidence Collection (US-014)

## Context Group
**Clinical Monitoring** — Observation + Surveillance + Evidence

## User Story
**As a** medical director  
**I want to** see evidence (video, photos) associated with an event  
**So that** I can verify what actually happened  

## Acceptance Criteria
- [ ] Video clip of the event
- [ ] Movement timeline
- [ ] Precise timestamps
- [ ] Links to stored evidence

## DSL Spec (Kotlin Test)

```kotlin
class EvidenceCollectionSpec : DescribeSpec({
    describe("US-014: View evidence for an event") {
        it("should create and retrieve evidence") {
            // Given
            val episode = surveillance.episodes.create("resident-5") {
                severity = EpisodeSeverity.CRITICAL
                title = "Fall with injury"
                evidenceKind = "clip"
            }

            // When
            val clipWindow = evidence.clipWindows.open(
                bedId = "bed-1",
                residentId = "resident-5"
            )
            val timeline = evidence.timelines.open(
                bedId = "bed-1",
                residentId = "resident-5"
            )

            // Then
            clipWindow shouldNotBeNull { it.state shouldBe "OPEN" }
            timeline shouldNotBeNull { it.isOpen shouldBe true }

            // When: close
            clipWindow.close()
            timeline.close()

            // Then
            clipWindow.state shouldBe "CLOSED"
            timeline.isOpen shouldBe false
        }
    }
})
```

## Status
📝 Pending implementation
