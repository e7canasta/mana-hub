package com.hub.clients.specs.residentlifecycle

import com.hub.clients.core.manahub
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import java.time.LocalDate

class ResidentAdmissionSpec : BehaviorSpec({

    Given("a new resident with demographic data") {
        val hub = manahub("http://localhost:8080") {}
        val birthDate = LocalDate.of(1935, 3, 15)
        val admissionDate = LocalDate.now()

        When("the resident is admitted") {
            val resident = hub.population.admitResident(
                fullName = "Maria Garcia Lopez",
                birthDate = birthDate,
                admissionDate = admissionDate
            )

            Then("the resident should be created with correct data") {
                resident.fullName shouldBe "Maria Garcia Lopez"
                resident.birthDate shouldBe birthDate
                resident.admissionDate shouldBe admissionDate
                resident.status.name shouldBe "ACTIVE"
                resident.isDischarged shouldBe false
            }

            Then("the resident should have an auto-generated id") {
                resident.id.shouldNotBeBlank()
            }
        }
    }

    Given("a resident admitted today") {
        val hub = manahub("http://localhost:8080") {}

        When("the resident is admitted") {
            val resident = hub.population.admitResident(
                fullName = "Juan Perez",
                birthDate = LocalDate.of(1940, 5, 20),
                admissionDate = LocalDate.now()
            )

            Then("the status should be ACTIVE") {
                resident.status.name shouldBe "ACTIVE"
                resident.isDischarged shouldBe false
            }
        }
    }

    Given("a resident with a specific birth date") {
        val hub = manahub("http://localhost:8080") {}
        val birthDate = LocalDate.of(1925, 12, 25)

        When("the resident is admitted") {
            val resident = hub.population.admitResident(
                fullName = "Christmas Baby",
                birthDate = birthDate,
                admissionDate = LocalDate.now()
            )

            Then("the birth date should be stored correctly") {
                resident.birthDate shouldBe birthDate
            }
        }
    }

    Given("a resident with a specific admission date") {
        val hub = manahub("http://localhost:8080") {}
        val admissionDate = LocalDate.of(2026, 1, 15)

        When("the resident is admitted") {
            val resident = hub.population.admitResident(
                fullName = "January Admit",
                birthDate = LocalDate.of(1940, 6, 15),
                admissionDate = admissionDate
            )

            Then("the admission date should be stored correctly") {
                resident.admissionDate shouldBe admissionDate
            }
        }
    }
})
