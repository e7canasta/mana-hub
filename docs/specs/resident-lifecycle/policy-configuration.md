# Spec: Policy Configuration (US-004, US-006)

## Context Group
**Resident Lifecycle** — Population + Policy + Surveillance

## User Stories

### US-004: Configure monitoring profile
**As a** medical director  
**I want to** define how each resident is monitored  
**So that** alerts are adjusted to their specific needs  

### US-006: Change monitoring profile
**As a** medical director  
**I want to** change the monitoring profile when a resident's condition changes  
**So that** monitoring adapts to their current state  

## Acceptance Criteria
- [ ] Select preset (fall_risk, wanderer, night_watch, default)
- [ ] Assign risk level (LOW, MEDIUM, HIGH)
- [ ] Indicate mobility aid (walker, wheelchair, none)
- [ ] Autopilot mode (on/off)
- [ ] Custom overrides (JSON)
- [ ] Temporal validity (validFrom, validTo)
- [ ] Change reason recorded

## DSL Spec (Kotlin Test)

```kotlin
class PolicyConfigurationSpec : DescribeSpec({
    val hub = ManaHubClient("http://localhost:8080")
    val session = hub.identity.login("admin", "admin123")
    val policy = hub.policy(session)

    describe("US-004: Configure monitoring profile") {
        it("should configure profile with preset and risk level") {
            // Given
            val resident = ...
            val catalog = policy.presets.catalog()

            // When
            val profile = policy.profiles.configure(resident.id) {
                mobilityAid = "walker"
                autopilot = true
                mode = "fall_risk"
                templateId = "elderly_fall_risk"
                riskLevel = RiskLevel.HIGH
            }

            // Then
            profile.residentId shouldBe resident.id
            profile.riskLevel shouldBe RiskLevel.HIGH
            profile.templateId shouldBe "elderly_fall_risk"
            profile.mobilityAid shouldBe "walker"
            profile.autopilot shouldBe true
        }
    }

    describe("US-006: Change monitoring profile") {
        it("should create new version and deactivate old") {
            // Given
            val resident = ...
            val profile1 = policy.profiles.configure(resident.id) {
                riskLevel = RiskLevel.LOW
            }

            // When
            val profile2 = policy.profiles.configure(resident.id) {
                riskLevel = RiskLevel.HIGH
            }

            // Then
            profile2.riskLevel shouldBe RiskLevel.HIGH
            val history = policy.profiles.history(resident.id)
            history shouldHaveSize 2
            history.first().riskLevel shouldBe RiskLevel.LOW
            history.last().riskLevel shouldBe RiskLevel.HIGH
        }
    }
})
```

## Scenarios

### Scenario 1: Configure profile
```gherkin
Given a medical director
And a resident "Maria"
When configuring profile with preset "elderly_fall_risk" and risk "HIGH"
Then the profile is created
And it becomes the current profile
```

### Scenario 2: Change profile
```gherkin
Given a resident "Maria" with profile risk "LOW"
When changing profile to risk "HIGH"
Then a new profile version is created
The old version is deactivated
And the new profile is current
```

## API Endpoints
- `GET /api/v1/alarm-presets/catalog` — List presets
- `PATCH /api/v1/alarm-presets/{residentId}` — Configure profile
- `GET /api/v1/alarm-presets/{residentId}` — Get current profile
- `GET /api/v1/alarm-presets/{residentId}/history` — Get history

## Domain Objects
- `AlarmProfile` — Entity
- `AlarmPreset` — Template
- `RiskLevel` — Enum: LOW, MEDIUM, HIGH
- `AlarmProfileBuilder` — DSL builder

## Status
📝 Pending implementation
