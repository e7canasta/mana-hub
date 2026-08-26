package com.hub.clients.specs.clinicalmonitoring

import com.hub.clients.core.manahub
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldNotBeEmpty
import java.time.LocalDate

class EvidenceCollectionSpec : BehaviorSpec({

    Given("a resident and a bed") {
        val hub = manahub("http://localhost:8080") {}
        val resident = hub.population.admitResident(
            fullName = "Maria Evidence",
            birthDate = LocalDate.of(1935, 3, 15),
            admissionDate = LocalDate.now()
        )
        val facility = hub.residence.setupFacility("Test Facility Evidence") {
            wing("Wing A") {
                room("101") {
                    bed("A")
                }
            }
        }
        val bed = facility.firstBed()

        When("evidence is created") {
            val evidence = hub.evidence.createEvidence(
                bedId = bed!!.id,
                residentId = resident.id,
                evidenceType = "VIDEO_CLIP",
                category = "fall_detection"
            )

            Then("the evidence should be created correctly") {
                evidence shouldNotBe null
                evidence.residentId shouldBe resident.id
                evidence.evidenceType shouldBe "VIDEO_CLIP"
            }
        }
    }

    Given("a resident and a bed") {
        val hub = manahub("http://localhost:8080") {}
        val resident = hub.population.admitResident(
            fullName = "Juan Timeline",
            birthDate = LocalDate.of(1940, 5, 20),
            admissionDate = LocalDate.now()
        )
        val facility = hub.residence.setupFacility("Test Facility Timeline") {
            wing("Wing B") {
                room("201") {
                    bed("A")
                }
            }
        }
        val bed = facility.firstBed()

        When("a timeline is opened") {
            val timeline = hub.evidence.openTimeline(bed!!.id, resident.id)

            Then("the timeline should be open") {
                timeline.isOpen shouldBe true
                timeline.residentId shouldBe resident.id
            }

            And("the timeline is closed") {
                val closed = hub.evidence.closeTimeline(timeline.id)

                Then("the timeline should be closed") {
                    closed.isOpen shouldBe false
                }
            }
        }
    }

    Given("a resident and a bed") {
        val hub = manahub("http://localhost:8080") {}
        val resident = hub.population.admitResident(
            fullName = "Ana Clip",
            birthDate = LocalDate.of(1928, 2, 14),
            admissionDate = LocalDate.now()
        )
        val facility = hub.residence.setupFacility("Test Facility Clip") {
            wing("Wing C") {
                room("301") {
                    bed("A")
                }
            }
        }
        val bed = facility.firstBed()

        When("a clip window is opened") {
            val clipWindow = hub.evidence.openClipWindow(bed!!.id, resident.id)

            Then("the clip window should be open") {
                clipWindow.state shouldBe "open"
                clipWindow.residentId shouldBe resident.id
            }

            And("the clip window is closed") {
                val closed = hub.evidence.closeClipWindow(clipWindow.id)

                Then("the clip window should be closed") {
                    closed.state shouldBe "closed"
                }
            }
        }
    }

    Given("a bed with an open clip window") {
        val hub = manahub("http://localhost:8080") {}
        val facility = hub.residence.setupFacility("Test Facility List") {
            wing("Wing D") {
                room("401") {
                    bed("A")
                }
            }
        }
        val bed = facility.firstBed()
        val resident = hub.population.admitResident(
            fullName = "Luis List",
            birthDate = LocalDate.of(1945, 9, 30),
            admissionDate = LocalDate.now()
        )
        hub.evidence.openClipWindow(bed!!.id, resident.id)

        When("open clip windows are listed for the bed") {
            val openWindows = hub.evidence.openClipWindows(bed.id)

            Then("the list should not be empty") {
                openWindows shouldNotBe null
                openWindows.isNotEmpty() shouldBe true
            }
        }
    }
})
