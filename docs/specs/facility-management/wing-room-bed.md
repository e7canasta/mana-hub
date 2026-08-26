# Spec: Wing Room Bed

## Context Group
**Facility Management** — Residence + Streams + Identity

## DSL Spec (Kotlin Test)

```kotlin
class WingRoomBedSpec : DescribeSpec({
    describe("Wing Room Bed management") {
        it("should list wings for a facility") {
            // Given
            val facility = residence.facilities.create("Test")
            facility.wings.create("Wing A")
            facility.wings.create("Wing B")

            // When
            val wings = facility.wings.list()

            // Then
            wings shouldHaveSize 2
        }

        it("should list rooms for a wing") {
            // Given
            val wing = facility.wings.create("Wing A")
            wing.rooms.create("101")
            wing.rooms.create("102")

            // When
            val rooms = wing.rooms.list()

            // Then
            rooms shouldHaveSize 2
        }

        it("should list beds for a room") {
            // Given
            val room = wing.rooms.create("101")
            room.beds.create("A")
            room.beds.create("B")

            // When
            val beds = room.beds.list()

            // Then
            beds shouldHaveSize 2
        }
    }
})
```

## Status
📝 Pending implementation
