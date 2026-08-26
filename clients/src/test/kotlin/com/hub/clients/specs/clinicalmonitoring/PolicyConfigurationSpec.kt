package com.hub.clients.specs.clinicalmonitoring

import com.hub.clients.core.manahub
import com.hub.clients.policy.RiskLevel
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldNotBeEmpty
import java.time.LocalDate

class PolicyConfigurationSpec : BehaviorSpec({

    Given("a resident with a fall risk profile") {
        val hub = manahub("http://localhost:8080") {}
        val resident = hub.population.admitResident(
            fullName = "Maria Policy",
            birthDate = LocalDate.of(1935, 3, 15),
            admissionDate = LocalDate.now()
        )

        When("the alarm profile is configured") {
            val profile = hub.policy.configureAlarmProfile(resident.id) {
                mobilityAid = "walker"
                autopilot = true
                mode = "fall_risk"
                templateId = "elderly_fall_risk"
                riskLevel = RiskLevel.HIGH
            }

            Then("the profile should have the correct settings") {
                profile.residentId shouldBe resident.id
                profile.riskLevel shouldBe RiskLevel.HIGH
                profile.templateId shouldBe "elderly_fall_risk"
                profile.autopilot shouldBe true
            }
        }
    }

    Given("a resident with an existing profile") {
        val hub = manahub("http://localhost:8080") {}
        val resident = hub.population.admitResident(
            fullName = "Juan Profile",
            birthDate = LocalDate.of(1940, 5, 20),
            admissionDate = LocalDate.now()
        )
        hub.policy.configureAlarmProfile(resident.id) {
            riskLevel = RiskLevel.MEDIUM
        }

        When("the current profile is retrieved") {
            val profile = hub.policy.alarmProfile(resident.id)

            Then("the profile should exist with the correct risk level") {
                profile shouldNotBe null
                profile?.riskLevel shouldBe RiskLevel.MEDIUM
            }
        }
    }

    Given("the alarm preset catalog") {
        val hub = manahub("http://localhost:8080") {}

        When("the catalog is queried") {
            val catalog = hub.policy.catalog()

            Then("the catalog should contain presets") {
                catalog.presets.shouldNotBeEmpty()
            }
        }
    }

    Given("a resident with a low risk profile") {
        val hub = manahub("http://localhost:8080") {}
        val resident = hub.population.admitResident(
            fullName = "Carlos History",
            birthDate = LocalDate.of(1938, 7, 10),
            admissionDate = LocalDate.now()
        )
        hub.policy.configureAlarmProfile(resident.id) {
            riskLevel = RiskLevel.LOW
        }

        When("a new profile is created with high risk") {
            hub.policy.configureAlarmProfile(resident.id) {
                riskLevel = RiskLevel.HIGH
            }

            Then("the history should contain both versions") {
                val history = hub.policy.alarmProfileHistory(resident.id)
                history.size shouldBe 2
            }
        }
    }
})
