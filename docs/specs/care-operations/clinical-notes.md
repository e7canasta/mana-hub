# Spec: Clinical Notes

## Context Group
**Care Operations** — Care + Surveillance + Audit

## User Story
**As a** nurse  
**I want to** add clinical notes during rounds  
**So that** care is documented  

## Acceptance Criteria
- [ ] Care note tied to round
- [ ] Resident note (finding/insight)
- [ ] Episode note
- [ ] Shift note

## DSL Spec (Kotlin Test)

```kotlin
class ClinicalNotesSpec : DescribeSpec({
    describe("Clinical notes") {
        it("should create care note during round") {
            // Given
            val round = care.rounds.create(wingId = "wing-1")

            // When
            care.careNotes.create(
                residentId = "resident-1",
                authorId = "nurse-1",
                body = "BP 140/90, stable"
            )

            // Then
            val notes = care.careNotes.list(residentId = "resident-1")
            notes shouldNotBeEmpty
            notes.last().body shouldBe "BP 140/90, stable"
        }

        it("should create resident note (finding)") {
            // When
            care.residentNotes.create(
                residentId = "resident-1",
                authorId = "doctor-1",
                kind = "INSIGHT",
                body = "Pattern: insomnia between 2-4 AM"
            )

            // Then
            val notes = care.residentNotes.list(residentId = "resident-1")
            notes shouldNotBeEmpty
            notes.last().kind shouldBe "INSIGHT"
        }
    }
})
```

## Status
📝 Pending implementation
