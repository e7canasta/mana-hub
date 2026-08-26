package com.hub.clients.specs.clinicalmonitoring

import com.hub.clients.core.manahub
import com.hub.clients.observation.BathroomSummaryData
import com.hub.clients.observation.MobilitySummaryData
import com.hub.clients.observation.PerceptionKind
import com.hub.clients.observation.SleepSummaryData
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldNotBeEmpty
import java.time.LocalDate

class PerceptionIngestionSpec : BehaviorSpec({

    Given("a sensor event") {
        val hub = manahub("http://localhost:8080") {}
        val resident = hub.population.admitResident(
            fullName = "Maria Perception",
            birthDate = LocalDate.of(1935, 3, 15),
            admissionDate = LocalDate.now()
        )

        When("the event is ingested") {
            hub.observation.ingestEvent(
                monitorKey = "cam-test",
                kind = PerceptionKind.LOCATION,
                bedId = "test-bed-1",
                residentId = resident.id,
                state = "out_of_bed",
                sleeping = false
            )

            Then("the event should be ingested without error") {
                // Event ingested without error
            }
        }
    }

    Given("a sleep summary") {
        val hub = manahub("http://localhost:8080") {}
        val resident = hub.population.admitResident(
            fullName = "Juan Sleep",
            birthDate = LocalDate.of(1940, 5, 20),
            admissionDate = LocalDate.now()
        )
        val data = SleepSummaryData(
            calmMinutes = 360,
            restlessMinutes = 45,
            awakeMinutes = 30,
            outOfBedMinutes = 15,
            bedExitCount = 2,
            wakeCount = 3
        )

        When("the sleep summary is ingested") {
            hub.observation.ingestSleepSummary(resident.id, LocalDate.now(), data)

            Then("the summary should be retrieved correctly") {
                val summary = hub.observation.sleepSummary(resident.id)
                summary shouldNotBe null
                summary?.calmMinutes shouldBe 360
            }
        }
    }

    Given("a mobility summary") {
        val hub = manahub("http://localhost:8080") {}
        val resident = hub.population.admitResident(
            fullName = "Carlos Mobility",
            birthDate = LocalDate.of(1938, 7, 10),
            admissionDate = LocalDate.now()
        )
        val data = MobilitySummaryData(
            inBedMinutes = 400,
            outOfBedMinutes = 40,
            outOfSightMinutes = 10,
            walkingMinutes = 20,
            distanceMeters = 150.0,
            transferCount = 3
        )

        When("the mobility summary is ingested") {
            hub.observation.ingestMobilitySummary(resident.id, LocalDate.now(), data)

            Then("the summary should be retrieved correctly") {
                val summary = hub.observation.mobilitySummary(resident.id)
                summary shouldNotBe null
                summary?.walkingMinutes shouldBe 20
            }
        }
    }

    Given("a bathroom summary") {
        val hub = manahub("http://localhost:8080") {}
        val resident = hub.population.admitResident(
            fullName = "Ana Bathroom",
            birthDate = LocalDate.of(1928, 2, 14),
            admissionDate = LocalDate.now()
        )
        val data = BathroomSummaryData(
            visitCount = 3,
            nightVisitCount = 2,
            assistedCount = 0,
            totalMinutes = 25
        )

        When("the bathroom summary is ingested") {
            hub.observation.ingestBathroomSummary(resident.id, LocalDate.now(), data)

            Then("the summary should be retrieved correctly") {
                val summary = hub.observation.bathroomSummary(resident.id)
                summary shouldNotBe null
                summary?.visitCount shouldBe 3
            }
        }
    }

    Given("an informational notification") {
        val hub = manahub("http://localhost:8080") {}

        When("the notification is ingested") {
            hub.observation.notifyInformational(
                category = "bed_exit",
                bedId = "test-bed-2",
                residentId = null,
                eventType = "visual_only",
                riskLevel = "low"
            )

            Then("the notification should be ingested without error") {
                // Notification ingested without error
            }
        }
    }

    Given("a resident with notifications") {
        val hub = manahub("http://localhost:8080") {}
        val resident = hub.population.admitResident(
            fullName = "Pedro Notifications",
            birthDate = LocalDate.of(1932, 11, 5),
            admissionDate = LocalDate.now()
        )
        hub.observation.notifyInformational(
            category = "test",
            residentId = resident.id,
            eventType = "test_event"
        )

        When("notifications are listed by resident") {
            val notifications = hub.observation.notificationsByResident(resident.id)

            Then("the list should not be empty") {
                notifications.shouldNotBeEmpty()
            }
        }
    }
})
