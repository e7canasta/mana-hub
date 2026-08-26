# Spec: Camera Stream

## Context Group
**Facility Management** — Residence + Streams + Identity

## DSL Spec (Kotlin Test)

```kotlin
class CameraStreamSpec : DescribeSpec({
    describe("Camera stream management") {
        it("should assign stream to room") {
            // Given
            val streams = hub.streams(session)

            // When
            val stream = streams.streams.create(roomId = "room-1") {
                streamKey = "cam-101"
                name = "Room 101 Camera"
            }

            // Then
            stream.streamKey shouldBe "cam-101"
            stream.active shouldBe true
        }

        it("should define regions") {
            // Given
            val stream = streams.streams.retrieve("stream-1")

            // When
            stream.defineRegions {
                bed(
                    points = listOf(Point2D(0, 0), Point2D(100, 0), Point2D(100, 100), Point2D(0, 100)),
                    label = "Bed"
                )
                bathroom(
                    points = listOf(Point2D(150, 0), Point2D(250, 0), Point2D(250, 100), Point2D(150, 100)),
                    label = "Bathroom"
                )
            }

            // Then
            val regions = stream.regions()
            regions shouldHaveSize 2
        }
    }
})
```

## Status
📝 Pending implementation
