package com.hub.clients.specs.core

import com.hub.clients.core.manahub
import com.hub.clients.care.ResidentNoteType
import com.hub.clients.observation.PerceptionKind
import com.hub.clients.observation.SleepSummaryData
import com.hub.clients.surveillance.EpisodeSeverity
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldNotBeEmpty
import java.time.LocalDate

class ClientDslSpec : BehaviorSpec({

    Given("the ManaHub client") {
        When("the client is created with default URL") {
            val hub = manahub("http://localhost:8080") {}

            Then("the client should not be null") {
                hub shouldNotBe null
            }
        }
    }

    Given("the ManaHub client") {
        When("all scopes are available") {
            val hub = manahub("http://localhost:8080") {}

            Then("all scopes should be accessible") {
                hub.identity shouldNotBe null
                hub.residence shouldNotBe null
                hub.population shouldNotBe null
                hub.surveillance shouldNotBe null
                hub.observation shouldNotBe null
                hub.evidence shouldNotBe null
                hub.care shouldNotBe null
                hub.history shouldNotBe null
                hub.policy shouldNotBe null
                hub.streams shouldNotBe null
                hub.audit shouldNotBe null
            }
        }
    }

    Given("admin user is registered") {
        When("the user is registered") {
            val hub = manahub("http://localhost:8080") {}
            val uniqueUsername = "test_user_${java.util.UUID.randomUUID().toString().substring(0, 8)}"
            val user = hub.identity.registerUser {
                username = uniqueUsername
                displayName = "Test User"
                role = com.hub.clients.identity.Role.STAFF
                password = "test123"
            }

            Then("the user should be created") {
                user shouldNotBe null
                user.username shouldBe uniqueUsername
            }
        }
    }

    Given("a facility setup request") {
        When("a facility is created with wings, rooms, beds") {
            val hub = manahub("http://localhost:8080") {}
            val facility = hub.residence.setupFacility("Test Facility") {
                timezone = "America/Mexico_City"
                wing("Piso 1") {
                    floor = "1"
                    room("101") {
                        bed("A")
                    }
                }
            }

            Then("the facility should be created") {
                facility shouldNotBe null
                facility.name shouldBe "Test Facility"
            }
        }
    }

    Given("a facility") {
        When("the facility tree is retrieved") {
            val hub = manahub("http://localhost:8080") {}
            val facility = hub.residence.setupFacility("Test Facility Tree") {
                wing("Wing A") {
                    room("101") { bed("A") }
                }
            }
            val tree = facility.tree()

            Then("the tree should not be empty") {
                tree shouldNotBe null
                tree.wings shouldNotBe emptyList<com.hub.clients.residence.WingTreeResponse>()
            }
        }
    }

    Given("a facility") {
        When("the first bed is retrieved") {
            val hub = manahub("http://localhost:8080") {}
            val facility = hub.residence.setupFacility("Test Facility Bed") {
                wing("Wing B") {
                    room("201") { bed("A") }
                }
            }
            val bed = facility.firstBed()

            Then("the bed should not be null") {
                bed shouldNotBe null
            }
        }
    }

    Given("a resident admission request") {
        When("the resident is admitted") {
            val hub = manahub("http://localhost:8080") {}
            val resident = hub.population.admitResident(
                fullName = "Test Resident",
                birthDate = LocalDate.of(1940, 5, 20),
                admissionDate = LocalDate.now()
            )

            Then("the resident should be created") {
                resident shouldNotBe null
                resident.fullName shouldBe "Test Resident"
            }
        }
    }

    Given("a resident") {
        When("an episode is triggered and resolved") {
            val hub = manahub("http://localhost:8080") {}
            val resident = hub.population.admitResident(
                fullName = "Episode Resident",
                birthDate = LocalDate.of(1935, 3, 15),
                admissionDate = LocalDate.now()
            )
            val episode = hub.surveillance.triggerEpisode(resident.id) {
                severity = EpisodeSeverity.WARNING
                title = "Test episode"
                detail = "Test detail"
            }

            Then("the episode should be created") {
                episode shouldNotBe null
                episode.severity shouldBe EpisodeSeverity.WARNING
            }

            And("the episode is acknowledged and resolved") {
                episode.acknowledge("nurse_test")
                episode.resolve("resolved")

                Then("the episode should be resolved") {
                    episode.status shouldBe "resolved"
                }
            }
        }
    }

    Given("a sensor event") {
        When("the event is ingested") {
            val hub = manahub("http://localhost:8080") {}
            hub.observation.ingestEvent(
                monitorKey = "cam-test",
                kind = PerceptionKind.POSTURE,
                bedId = "test-bed-1",
                residentId = null,
                state = "in_bed",
                sleeping = true
            )

            Then("the event should be ingested without error") {
                // Event ingested without error
            }
        }
    }

    Given("a resident with sleep summary") {
        When("the clinical summary is retrieved") {
            val hub = manahub("http://localhost:8080") {}
            val resident = hub.population.admitResident(
                fullName = "Summary Resident",
                birthDate = LocalDate.of(1938, 7, 10),
                admissionDate = LocalDate.now()
            )
            hub.observation.ingestSleepSummary(resident.id, LocalDate.now(), SleepSummaryData(
                calmMinutes = 360,
                restlessMinutes = 45,
                awakeMinutes = 30,
                outOfBedMinutes = 15,
                bedExitCount = 2,
                wakeCount = 3
            ))
            val summary = hub.observation.sleepSummary(resident.id)

            Then("the summary should not be null") {
                summary shouldNotBe null
                summary?.calmMinutes shouldBe 360
            }
        }
    }

    Given("a resident and a bed") {
        When("a timeline is opened and closed") {
            val hub = manahub("http://localhost:8080") {}
            val resident = hub.population.admitResident(
                fullName = "Evidence Resident",
                birthDate = LocalDate.of(1932, 11, 5),
                admissionDate = LocalDate.now()
            )
            val facility = hub.residence.setupFacility("Test Facility Evidence") {
                wing("Wing A") {
                    room("101") { bed("A") }
                }
            }
            val bed = facility.firstBed()
            val timeline = hub.evidence.openTimeline(bed!!.id, resident.id)

            Then("the timeline should be open") {
                timeline.isOpen shouldBe true
            }

            And("the timeline is closed") {
                val closed = hub.evidence.closeTimeline(timeline.id)

                Then("the timeline should be closed") {
                    closed.isOpen shouldBe false
                }
            }
        }
    }

    Given("a facility with a wing") {
        When("a round is started and completed") {
            val hub = manahub("http://localhost:8080") {}
            val facility = hub.residence.setupFacility("Test Facility Round") {
                wing("Wing A") {
                    room("101") { bed("A") }
                }
            }
            val wing = facility.tree().wings.first()
            val round = hub.care.startRound(wing.wing.id)

            Then("the round should be in progress") {
                round shouldNotBe null
                round.status.toString() shouldBe "IN_PROGRESS"
            }

            And("the round is completed") {
                val completed = round.complete("nurse_test")

                Then("the round should be completed") {
                    completed.status.toString() shouldBe "COMPLETED"
                }
            }
        }
    }

    Given("a resident") {
        When("a resident note is added (kind=CARE)") {
            val hub = manahub("http://localhost:8080") {}
            val resident = hub.population.admitResident(
                fullName = "Care Resident",
                birthDate = LocalDate.of(1945, 9, 30),
                admissionDate = LocalDate.now()
            )
            val note = hub.care.addResidentNote(
                residentId = resident.id,
                authorId = "nurse_test",
                kind = ResidentNoteType.CARE,
                body = "Test note"
            )

            Then("the note should be created") {
                note shouldNotBe null
                note.body shouldBe "Test note"
            }
        }
    }

    Given("a room") {
        When("a stream is assigned") {
            val hub = manahub("http://localhost:8080") {}
            val facility = hub.residence.setupFacility("Test Facility Stream") {
                wing("Wing A") {
                    room("101") { bed("A") }
                }
            }
            val tree = facility.tree()
            val room = tree.wings.first().rooms.first()
            val stream = hub.streams.assignStreamToRoom(room.room.id) {
                streamKey = "cam-101"
                name = "Camera 101"
            }

            Then("the stream should be created") {
                stream shouldNotBe null
                stream.streamKey shouldBe "cam-101"
            }
        }
    }

    Given("a resident with incidents") {
        When("incidents are listed") {
            val hub = manahub("http://localhost:8080") {}
            val resident = hub.population.admitResident(
                fullName = "Incident Resident",
                birthDate = LocalDate.of(1930, 1, 1),
                admissionDate = LocalDate.now()
            )
            hub.surveillance.triggerEpisode(resident.id) {
                title = "Test incident"
            }
            val episodes = hub.surveillance.episodesByResident(resident.id)

            Then("the list should not be empty") {
                episodes shouldNotBe emptyList<com.hub.clients.surveillance.EpisodeResponse>()
            }
        }
    }

    Given("the audit log") {
        When("audit is queried by entity") {
            val hub = manahub("http://localhost:8080") {}
            val log = hub.audit.byEntity("Facility", "test-id")

            Then("the log should not be null") {
                log shouldNotBe null
            }
        }
    }

    Given("the audit log") {
        When("audit is queried by actor") {
            val hub = manahub("http://localhost:8080") {}
            val log = hub.audit.byActor("test-actor")

            Then("the log should not be null") {
                log shouldNotBe null
            }
        }
    }
})
