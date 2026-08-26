package com.hub.clients.specs.care

import com.hub.clients.core.manahub
import com.hub.clients.care.ResidentNoteType
import com.hub.clients.care.ShiftNoteKind
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldNotBeEmpty
import java.time.LocalDate

class ClinicalNotesSpec : BehaviorSpec({

    Given("a resident") {
        val hub = manahub("http://localhost:8080") {}
        val resident = hub.population.admitResident(
            fullName = "Maria Notes",
            birthDate = LocalDate.of(1935, 3, 15),
            admissionDate = LocalDate.now()
        )

        When("a care note is added (kind=CARE)") {
            val note = hub.care.addResidentNote(
                residentId = resident.id,
                authorId = "nurse_test",
                kind = ResidentNoteType.CARE,
                body = "Residente duerme bien"
            )

            Then("the note should be created correctly") {
                note shouldNotBe null
                note.body shouldBe "Residente duerme bien"
                note.kind shouldBe ResidentNoteType.CARE
            }
        }
    }

    Given("a resident with care notes") {
        val hub = manahub("http://localhost:8080") {}
        val resident = hub.population.admitResident(
            fullName = "Juan Notes",
            birthDate = LocalDate.of(1940, 5, 20),
            admissionDate = LocalDate.now()
        )
        hub.care.addResidentNote(
            residentId = resident.id,
            authorId = "nurse_test",
            kind = ResidentNoteType.CARE,
            body = "Test note"
        )

        When("resident notes are listed") {
            val notes = hub.care.residentNotes(resident.id)

            Then("the list should not be empty") {
                notes.shouldNotBeEmpty()
            }
        }
    }

    Given("a resident") {
        val hub = manahub("http://localhost:8080") {}
        val resident = hub.population.admitResident(
            fullName = "Carlos Patterns",
            birthDate = LocalDate.of(1938, 7, 10),
            admissionDate = LocalDate.now()
        )

        When("a pattern note is added (kind=PATTERN)") {
            val note = hub.care.addResidentNote(
                residentId = resident.id,
                authorId = "nurse_test",
                kind = ResidentNoteType.PATTERN,
                body = "Patron de sueno regular"
            )

            Then("the note should be created correctly") {
                note shouldNotBe null
                note.kind shouldBe ResidentNoteType.PATTERN
            }
        }
    }

    Given("a resident") {
        val hub = manahub("http://localhost:8080") {}
        val resident = hub.population.admitResident(
            fullName = "Ana Insights",
            birthDate = LocalDate.of(1928, 2, 14),
            admissionDate = LocalDate.now()
        )

        When("an insight note is added (kind=INSIGHT)") {
            val note = hub.care.addResidentNote(
                residentId = resident.id,
                authorId = "nurse_test",
                kind = ResidentNoteType.INSIGHT,
                body = "Residente prefiere ducha por la manana"
            )

            Then("the note should be created correctly") {
                note shouldNotBe null
                note.kind shouldBe ResidentNoteType.INSIGHT
            }
        }
    }

    Given("a resident") {
        val hub = manahub("http://localhost:8080") {}
        val resident = hub.population.admitResident(
            fullName = "Pedro Clinical",
            birthDate = LocalDate.of(1932, 11, 5),
            admissionDate = LocalDate.now()
        )

        When("a clinical note is added (kind=CLINICAL)") {
            val note = hub.care.addResidentNote(
                residentId = resident.id,
                authorId = "nurse_test",
                kind = ResidentNoteType.CLINICAL,
                body = "Presion arterial controlada"
            )

            Then("the note should be created correctly") {
                note shouldNotBe null
                note.kind shouldBe ResidentNoteType.CLINICAL
            }
        }
    }

    Given("a facility with a wing") {
        val hub = manahub("http://localhost:8080") {}
        val facility = hub.residence.setupFacility("Test Facility Shift") {
            wing("Wing B") {
                room("201") { bed("A") }
            }
        }
        val wing = facility.tree().wings.first()

        When("a shift note is added") {
            val note = hub.care.addShiftNote(
                facilityId = facility.id,
                wingId = wing.wing.id,
                shiftKey = "night",
                shiftDate = LocalDate.now().toString(),
                authorId = "nurse_test",
                kind = ShiftNoteKind.SHIFT_SUMMARY,
                body = "Turno sin incidentes"
            )

            Then("the note should be created correctly") {
                note shouldNotBe null
                note.kind shouldBe ShiftNoteKind.SHIFT_SUMMARY
            }
        }
    }

    Given("a resident with incidents") {
        val hub = manahub("http://localhost:8080") {}
        val resident = hub.population.admitResident(
            fullName = "Luis Incidents",
            birthDate = LocalDate.of(1945, 9, 30),
            admissionDate = LocalDate.now()
        )
        hub.surveillance.triggerEpisode(resident.id) {
            title = "Test incident"
        }

        When("incidents are listed") {
            val episodes = hub.surveillance.episodesByResident(resident.id)

            Then("the list should not be empty") {
                episodes shouldNotBe emptyList<com.hub.clients.surveillance.EpisodeResponse>()
            }
        }
    }
})
