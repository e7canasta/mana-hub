package com.hub.clients.specs.care

import com.hub.clients.core.manahub
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldNotBeEmpty
import java.time.LocalDate

class RoundCreationSpec : BehaviorSpec({

    Given("a facility with a wing") {
        val hub = manahub("http://localhost:8080") {}
        val facility = hub.residence.setupFacility("Test Facility Round") {
            wing("Wing A") {
                room("101") { bed("A") }
            }
        }
        val wing = facility.tree().wings.first()

        When("a round is started") {
            val round = hub.care.startRound(wing.wing.id)

            Then("the round should be in progress") {
                round shouldNotBe null
                round.status.toString() shouldBe "IN_PROGRESS"
            }
        }
    }

    Given("a facility with a wing") {
        val hub = manahub("http://localhost:8080") {}
        val facility = hub.residence.setupFacility("Test Facility Current") {
            wing("Wing B") {
                room("201") { bed("A") }
            }
        }
        val wing = facility.tree().wings.first()
        hub.care.startRound(wing.wing.id)

        When("the current round is retrieved") {
            val current = hub.care.currentRound(wing.wing.id)

            Then("the current round should exist") {
                current shouldNotBe null
            }
        }
    }

    Given("a facility with a wing") {
        val hub = manahub("http://localhost:8080") {}
        val facility = hub.residence.setupFacility("Test Facility List") {
            wing("Wing C") {
                room("301") { bed("A") }
            }
        }
        val wing = facility.tree().wings.first()
        hub.care.startRound(wing.wing.id)

        When("rounds are listed") {
            val rounds = hub.care.rounds(wing.wing.id)

            Then("the list should not be empty") {
                rounds shouldNotBe emptyList<com.hub.clients.care.RoundResponse>()
            }
        }
    }

    Given("a started round") {
        val hub = manahub("http://localhost:8080") {}
        val facility = hub.residence.setupFacility("Test Facility Complete") {
            wing("Wing D") {
                room("401") { bed("A") }
            }
        }
        val wing = facility.tree().wings.first()
        val round = hub.care.startRound(wing.wing.id)

        When("the round is completed") {
            val completed = round.complete("nurse_test")

            Then("the round should be completed") {
                completed.status.toString() shouldBe "COMPLETED"
                completed.completedBy shouldBe "nurse_test"
            }
        }
    }

    Given("a started round") {
        val hub = manahub("http://localhost:8080") {}
        val facility = hub.residence.setupFacility("Test Facility Tasks") {
            wing("Wing E") {
                room("501") { bed("A") }
            }
        }
        val wing = facility.tree().wings.first()
        val round = hub.care.startRound(wing.wing.id)

        When("a task is completed") {
            Then("the round should be in progress") {
                round.status.toString() shouldBe "IN_PROGRESS"
            }
        }
    }
})
