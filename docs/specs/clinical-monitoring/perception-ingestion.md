# Spec: Perception Ingestion

## Context Group
**Clinical Monitoring** — Observation + Surveillance + Evidence

## User Story
**As a** perception engine  
**I want to** ingest sensor events into mana-hub  
**So that** they are persisted for analysis  

## Acceptance Criteria
- [ ] Event is persisted with timestamp
- [ ] CurrentBedState is updated
- [ ] Event is linked to room, stream, and resident

## DSL Spec (Kotlin Test)

```kotlin
class PerceptionIngestionSpec : DescribeSpec({
    describe("Perception ingestion") {
        it("should ingest sensor event and update bed state") {
            // Given
            val observation = hub.observation(session)

            // When
            observation.ingestEvent(
                monitorKey = "cam-101",
                kind = "bed_exit",
                bedId = "facility-1:wing-1:room-101:bed-A",
                residentId = "resident-1",
                state = "out_of_bed",
                sleeping = false
            )

            // Then
            val bedState = observation.bedStates.retrieve("facility-1:wing-1:room-101:bed-A")
            bedState.state shouldBe "OUT_OF_BED"
            bedState.sleeping shouldBe false
        }
    }
})
```

## API Endpoints
- `POST /internal/v1/events` — Ingest sensor event

## Status
📝 Pending implementation
