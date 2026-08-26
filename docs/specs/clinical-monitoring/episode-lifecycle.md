# Spec: Episode Lifecycle (US-007, US-008, US-009)

## Context Group
**Clinical Monitoring** — Observation + Surveillance + Evidence

## User Stories

### US-007: View pending episodes
**As a** medical director  
**I want to** see all episodes pending attention  
**So that** I can prioritize what to address first  

### US-008: Review and resolve episode
**As a** medical director  
**I want to** see episode detail, evidence, and mark as resolved  
**So that** I can document what happened and what was done  

### US-009: View episodes for a resident
**As a** medical director  
**I want to** see the episode history for a resident  
**So that** I can identify recurring patterns  

## DSL Spec (Kotlin Test)

```kotlin
class EpisodeLifecycleSpec : DescribeSpec({
    describe("US-007: View pending episodes") {
        it("should list episodes sorted by severity") {
            // Given
            val episode1 = surveillance.episodes.create("resident-1") {
                severity = EpisodeSeverity.WARNING
                title = "Out of bed"
            }
            val episode2 = surveillance.episodes.create("resident-2") {
                severity = EpisodeSeverity.CRITICAL
                title = "Fall detected"
            }

            // When
            val pending = surveillance.episodes.list()
                .filter { it.isPending }
                .sortedByDescending { it.severity }

            // Then
            pending shouldNotBeEmpty
            pending.first().severity shouldBe EpisodeSeverity.CRITICAL
        }
    }

    describe("US-008: Review and resolve episode") {
        it("should acknowledge, add note, and resolve") {
            // Given
            val episode = surveillance.episodes.create("resident-3") {
                severity = EpisodeSeverity.WARNING
                title = "Night disorientation"
            }

            // When: acknowledge
            episode.acknowledge("nurse-1")
            episode.status shouldBe "ACKNOWLEDGED"

            // When: add note
            episode.notes.create("nurse-1", "CLINICAL_NOTE", "Resident oriented after intervention")

            // When: resolve
            episode.resolve("resolved")
            episode.status shouldBe "RESOLVED"
            episode.isPending shouldBe false
        }
    }

    describe("US-009: View episodes for a resident") {
        it("should list all episodes for a resident") {
            // Given
            surveillance.episodes.create("resident-4") { severity = EpisodeSeverity.WARNING }
            surveillance.episodes.create("resident-4") { severity = EpisodeSeverity.CRITICAL }

            // When
            val episodes = surveillance.episodes.list()
                .filter { it.residentId == "resident-4" }

            // Then
            episodes shouldHaveSize 2
        }
    }
})
```

## Status
📝 Pending implementation
