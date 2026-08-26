# Spec: Resident Admission (US-001)

## Context Group
**Resident Lifecycle** — Population + Policy + Surveillance

## User Story
**As a** medical director  
**I want to** register a new resident with demographic data and initial diagnosis  
**So that** I can begin monitoring them  

## Acceptance Criteria
- [ ] Full name, date of birth, admission date
- [ ] Initial diagnosis (free text)
- [ ] Status: ACTIVE
- [ ] Bed assignment (optional at admission)
- [ ] Auto-generated ID

## DSL Spec (Kotlin Test)

```kotlin
class ResidentAdmissionSpec : DescribeSpec({
    val hub = ManaHubClient("http://localhost:8080")
    val session = hub.identity.login("admin", "admin123")
    val population = hub.population(session)

    describe("US-001: Admit a new resident") {
        it("should create resident with demographic data") {
            // Given
            val birthDate = LocalDate.of(1935, 3, 15)
            val admissionDate = LocalDate.now()

            // When
            val resident = population.residents.create(
                fullName = "Maria Garcia Lopez",
                birthDate = birthDate,
                admissionDate = admissionDate
            )

            // Then
            resident.fullName shouldBe "Maria Garcia Lopez"
            resident.birthDate shouldBe birthDate
            resident.admissionDate shouldBe admissionDate
            resident.status shouldBe ResidentStatus.ACTIVE
            resident.isDischarged shouldBe false
            resident.id shouldNotBeBlank()
        }

        it("should auto-generate id") {
            // When
            val resident = population.residents.create(
                fullName = "Juan Perez",
                birthDate = LocalDate.of(1940, 5, 20),
                admissionDate = LocalDate.now()
            )

            // Then
            resident.id shouldNotBeBlank()
        }
    }
})
```

## Scenarios

### Scenario 1: Successful admission
```gherkin
Given an authenticated medical director
When registering a resident with name "Maria Garcia Lopez", birth 1935-03-15, admission today
Then a resident is created with status ACTIVE
And an id is auto-generated
```

### Scenario 2: Admission with bed assignment
```gherkin
Given an authenticated medical director
And a room "101" with bed "A" available
When registering a resident and assigning to bed "A"
Then the resident is assigned to bed "A"
And the bed status changes to OCCUPIED
```

## API Endpoints
- `POST /api/v1/residents` — Create resident
- `GET /api/v1/residents/{id}` — Get resident
- `GET /api/v1/residents` — List residents

## Domain Objects
- `Resident` — Aggregate root
- `ResidentStatus` — Enum: ACTIVE, INACTIVE, DECEASED
- `CreateResidentRequest` — Input DTO
- `ResidentResponse` — Output DTO

## Dependencies
- **Residence** — For bed assignment (optional)
- **Identity** — For authentication

## Status
📝 Pending implementation
