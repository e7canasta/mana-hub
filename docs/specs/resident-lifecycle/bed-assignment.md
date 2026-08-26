# Spec: Bed Assignment (US-002)

## Context Group
**Resident Lifecycle** — Population + Policy + Surveillance

## User Story
**As a** medical director  
**I want to** assign a resident to a specific bed  
**So that** the system knows where they are and can monitor them  

## Acceptance Criteria
- [ ] A resident can only be in one bed at a time
- [ ] Bed must be AVAILABLE
- [ ] Assignment date is recorded
- [ ] Can change beds (with history)

## DSL Spec (Kotlin Test)

```kotlin
class BedAssignmentSpec : DescribeSpec({
    val hub = ManaHubClient("http://localhost:8080")
    val session = hub.identity.login("admin", "admin123")
    val residence = hub.residence(session)
    val population = hub.population(session)

    describe("US-002: Assign resident to bed") {
        it("should assign resident to available bed") {
            // Given
            val facility = residence.facilities.create("Test Facility")
            val wing = facility.wings.create("Wing A")
            val room = wing.rooms.create("101")
            val bed = room.beds.create("A")
            val resident = population.residents.create(
                fullName = "Maria Garcia",
                birthDate = LocalDate.of(1935, 3, 15),
                admissionDate = LocalDate.now()
            )

            // When
            val assignment = resident.assignTo(bed.id)

            // Then
            assignment.residentId shouldBe resident.id
            assignment.bedId shouldBe bed.id
            assignment.active shouldBe true
        }

        it("should not allow double assignment") {
            // Given
            val resident = population.residents.create(...)
            val bed1 = ...
            val bed2 = ...
            resident.assignTo(bed1.id)

            // When/Then
            shouldThrow<Exception> {
                resident.assignTo(bed2.id)
            }
        }

        it("should allow bed change with history") {
            // Given
            val resident = population.residents.create(...)
            val bed1 = ...
            val bed2 = ...
            resident.assignTo(bed1.id)

            // When
            resident.assignTo(bed2.id)

            // Then
            val assignments = resident.assignments.list()
            assignments shouldHaveSize 2
            assignments.last().bedId shouldBe bed2.id
            assignments.last().active shouldBe true
            assignments.first().active shouldBe false
        }
    }
})
```

## Scenarios

### Scenario 1: Successful assignment
```gherkin
Given a resident "Maria" not assigned to any bed
And a bed "A" with status AVAILABLE
When assigning "Maria" to bed "A"
Then the assignment is created
And the bed status changes to OCCUPIED
```

### Scenario 2: Bed change
```gherkin
Given a resident "Maria" assigned to bed "A"
And a bed "B" with status AVAILABLE
When changing "Maria" to bed "B"
Then the old assignment is released
And a new assignment is created
And bed "A" returns to AVAILABLE
And bed "B" changes to OCCUPIED
```

## API Endpoints
- `POST /api/v1/residents/{id}/assignments` — Create assignment
- `GET /api/v1/residents/{id}/assignments` — List assignments

## Domain Objects
- `Assignment` — Entity
- `CreateAssignmentRequest` — Input DTO
- `AssignmentResponse` — Output DTO

## Status
📝 Pending implementation
