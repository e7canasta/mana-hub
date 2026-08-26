package com.hub.clients.specs.clinicalmonitoring

import com.hub.clients.care.EpisodeNoteKind
import com.hub.clients.core.manahub
import com.hub.clients.surveillance.EpisodeSeverity
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import java.time.LocalDate

class EpisodeLifecycleSpec : BehaviorSpec({

    Given("a resident with episodes") {
        val hub = manahub("http://localhost:8080") {}
        val resident = hub.population.admitResident(
            fullName = "Maria Episodes",
            birthDate = LocalDate.of(1935, 3, 15),
            admissionDate = LocalDate.now()
        )
        hub.surveillance.triggerEpisode(resident.id) {
            severity = EpisodeSeverity.WARNING
            title = "Test episode"
        }

        When("all episodes are listed") {
            val episodes = hub.surveillance.episodes()

            Then("the list should not be empty") {
                episodes.shouldNotBeEmpty()
            }
        }
    }

    Given("a resident with a critical episode") {
        val hub = manahub("http://localhost:8080") {}
        val resident = hub.population.admitResident(
            fullName = "Juan Severity",
            birthDate = LocalDate.of(1940, 5, 20),
            admissionDate = LocalDate.now()
        )
        hub.surveillance.triggerEpisode(resident.id) {
            severity = EpisodeSeverity.CRITICAL
            title = "Critical episode"
        }

        When("episodes are filtered by critical severity") {
            val criticalEpisodes = hub.surveillance.episodesBySeverity(EpisodeSeverity.CRITICAL)

            Then("all episodes should be critical") {
                criticalEpisodes.shouldNotBeEmpty()
                criticalEpisodes.all { it.severity == EpisodeSeverity.CRITICAL } shouldBe true
            }
        }
    }

    Given("a resident with episodes") {
        val hub = manahub("http://localhost:8080") {}
        val resident = hub.population.admitResident(
            fullName = "Carlos Resident",
            birthDate = LocalDate.of(1938, 7, 10),
            admissionDate = LocalDate.now()
        )
        hub.surveillance.triggerEpisode(resident.id) {
            severity = EpisodeSeverity.WARNING
            title = "Resident episode"
        }

        When("episodes are filtered by resident") {
            val residentEpisodes = hub.surveillance.episodesByResident(resident.id)

            Then("all episodes should belong to that resident") {
                residentEpisodes.shouldNotBeEmpty()
                residentEpisodes.all { it.residentId == resident.id } shouldBe true
            }
        }
    }

    Given("a pending episode") {
        val hub = manahub("http://localhost:8080") {}
        val resident = hub.population.admitResident(
            fullName = "Pedro Acknowledge",
            birthDate = LocalDate.of(1932, 11, 5),
            admissionDate = LocalDate.now()
        )
        val episode = hub.surveillance.triggerEpisode(resident.id) {
            severity = EpisodeSeverity.WARNING
            title = "Acknowledge test"
        }

        When("the episode is acknowledged") {
            episode.acknowledge("nurse_1")

            Then("the episode status should be acknowledged") {
                episode.status shouldBe "acknowledged"
            }
        }
    }

    Given("a pending episode") {
        val hub = manahub("http://localhost:8080") {}
        val resident = hub.population.admitResident(
            fullName = "Ana Note",
            birthDate = LocalDate.of(1928, 2, 14),
            admissionDate = LocalDate.now()
        )
        val episode = hub.surveillance.triggerEpisode(resident.id) {
            severity = EpisodeSeverity.WARNING
            title = "Note test"
        }

        When("a note is added to the episode") {
            episode.addNote("nurse_1", EpisodeNoteKind.CLINICAL_NOTE, "Residente estable")

            Then("the episode should exist") {
                episode shouldNotBe null
            }
        }
    }

    Given("a pending episode") {
        val hub = manahub("http://localhost:8080") {}
        val resident = hub.population.admitResident(
            fullName = "Luis Resolve",
            birthDate = LocalDate.of(1945, 9, 30),
            admissionDate = LocalDate.now()
        )
        val episode = hub.surveillance.triggerEpisode(resident.id) {
            severity = EpisodeSeverity.WARNING
            title = "Resolve test"
        }

        When("the episode is resolved") {
            episode.resolve("resolved")

            Then("the episode status should be resolved") {
                episode.status shouldBe "resolved"
                episode.isPending shouldBe false
            }
        }
    }

    Given("a resident with two episodes") {
        val hub = manahub("http://localhost:8080") {}
        val resident = hub.population.admitResident(
            fullName = "Maria Filter",
            birthDate = LocalDate.of(1935, 3, 15),
            admissionDate = LocalDate.now()
        )
        hub.surveillance.triggerEpisode(resident.id) {
            severity = EpisodeSeverity.WARNING
            title = "Episode 1"
        }
        hub.surveillance.triggerEpisode(resident.id) {
            severity = EpisodeSeverity.CRITICAL
            title = "Episode 2"
        }

        When("episodes are filtered by resident") {
            val residentEpisodes = hub.surveillance.episodesByResident(resident.id)

            Then("both episodes should be returned") {
                residentEpisodes shouldHaveSize 2
            }
        }
    }
})
