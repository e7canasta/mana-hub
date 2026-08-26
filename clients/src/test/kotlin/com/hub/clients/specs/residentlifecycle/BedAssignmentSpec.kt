package com.hub.clients.specs.residentlifecycle

import com.hub.clients.core.manahub
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import java.time.LocalDate

class BedAssignmentSpec : BehaviorSpec({

    Given("a facility with available beds") {
        val hub = manahub("http://localhost:8080") {}
        val facility = hub.residence.setupFacility("Test Facility Assign") {
            wing("Wing A") {
                room("101") {
                    bed("A")
                }
            }
        }
        val bed = facility.firstBed()

        And("a resident admitted") {
            val resident = hub.population.admitResident(
                fullName = "Maria Assignment",
                birthDate = LocalDate.of(1935, 3, 15),
                admissionDate = LocalDate.now()
            )

            When("the resident is assigned to a bed") {
                val assignment = resident.assignTo(bed!!.id)

                Then("the assignment should be created") {
                    assignment.residentId shouldBe resident.id
                    assignment.bedId shouldBe bed.id
                    assignment.isOpen shouldBe true
                }
            }
        }
    }

    Given("a resident assigned to a bed") {
        val hub = manahub("http://localhost:8080") {}
        val facility = hub.residence.setupFacility("Test Facility List") {
            wing("Wing B") {
                room("201") {
                    bed("A")
                }
            }
        }
        val bed = facility.firstBed()
        val resident = hub.population.admitResident(
            fullName = "Juan ListAssign",
            birthDate = LocalDate.of(1940, 5, 20),
            admissionDate = LocalDate.now()
        )
        resident.assignTo(bed!!.id)

        When("the assignments are listed") {
            val assignments = resident.assignments()

            Then("the list should not be empty") {
                assignments shouldNotBe null
                assignments.size.shouldBeGreaterThanOrEqualTo(1)
            }
        }
    }

    Given("a facility with two beds") {
        val hub = manahub("http://localhost:8080") {}
        val facility = hub.residence.setupFacility("Test Facility Change") {
            wing("Wing C") {
                room("301") {
                    bed("A")
                    bed("B")
                }
            }
        }
        val tree = facility.tree()
        val beds = tree.wings.first().rooms.first().beds
        val bedA = beds[0]
        val bedB = beds[1]

        And("a resident assigned to bed A") {
            val resident = hub.population.admitResident(
                fullName = "Carlos Change",
                birthDate = LocalDate.of(1938, 7, 10),
                admissionDate = LocalDate.now()
            )
            resident.assignTo(bedA.id)

            When("the resident is reassigned to bed B") {
                resident.assignTo(bedB.id)

                Then("the last assignment should be bed B") {
                    val assignments = resident.assignments()
                    assignments.size.shouldBeGreaterThanOrEqualTo(2)
                    assignments.last().bedId shouldBe bedB.id
                    assignments.last().isOpen shouldBe true
                }
            }
        }
    }

    Given("a resident assigned to a bed") {
        val hub = manahub("http://localhost:8080") {}
        val facility = hub.residence.setupFacility("Test Facility Discharge") {
            wing("Wing D") {
                room("401") {
                    bed("A")
                }
            }
        }
        val bed = facility.firstBed()
        val resident = hub.population.admitResident(
            fullName = "Pedro Discharge",
            birthDate = LocalDate.of(1932, 11, 5),
            admissionDate = LocalDate.now()
        )
        resident.assignTo(bed!!.id)

        When("the resident is discharged") {
            resident.discharge()

            Then("the resident should be marked as discharged") {
                resident.isDischarged shouldBe true
            }
        }
    }
})
