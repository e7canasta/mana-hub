# Spec: Scene Change

## Context Group
**Clinical Monitoring** — Observation + Surveillance + Evidence

## User Story
**As a** perception engine  
**I want to** confirm scene changes after hysteresis  
**So that** only stable transitions are recorded  

## Acceptance Criteria
- [ ] Scene change is persisted
- [ ] From/to states are recorded
- [ ] Linked to room, stream, and resident

## DSL Spec (Kotlin Test)

```kotlin
class SceneChangeSpec : DescribeSpec({
    describe("Scene change") {
        it("should record confirmed scene transition") {
            // Given
            val observation = hub.observation(session)

            // When
            observation.ingestSceneEvent(
                monitorKey = "cam-101",
                fromState = "in_bed",
                toState = "out_of_bed",
                residentId = "resident-1"
            )

            // Then
            val events = observation.sceneEvents.list(residentId = "resident-1")
            events shouldNotBeEmpty
            events.last().fromState shouldBe "in_bed"
            events.last().toState shouldBe "out_of_bed"
        }
    }
})
```

## Status
📝 Pending implementation
