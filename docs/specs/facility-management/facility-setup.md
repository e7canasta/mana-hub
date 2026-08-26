# Spec: Facility Setup

## Context Group
**Facility Management** — Residence + Streams + Identity

## User Story
**As a** medical director  
**I want to** set up a facility with wings, rooms, and beds  
**So that** the physical layout is reflected in the system  

## DSL Spec (Kotlin Test)

```kotlin
class FacilitySetupSpec : DescribeSpec({
    describe("Facility setup") {
        it("should create facility hierarchy") {
            // Given
            val residence = hub.residence(session)

            // When
            val facility = residence.facilities.create("Residencia Esperanza") {
                timezone = "America/Mexico_City"
            }
            val wing = facility.wings.create("Piso 1", floor = 1)
            val room = wing.rooms.create("101")
            val bed = room.beds.create("A")

            // Then
            facility.name shouldBe "Residencia Esperanza"
            wing.name shouldBe "Piso 1"
            room.number shouldBe "101"
            bed.label shouldBe "A"
        }
    }
})
```

## Status
📝 Pending implementation
