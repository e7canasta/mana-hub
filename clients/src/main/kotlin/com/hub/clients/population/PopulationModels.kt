package com.hub.clients.population

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.time.LocalDate

data class CreateResidentRequest(
    val fullName: String,
    val birthDate: LocalDate? = null,
    val admissionDate: LocalDate,
    val externalId: String? = null
)

data class ResidentResponse(
    val id: String,
    @JsonProperty("fullName") val fullName: String,
    val birthDate: LocalDate? = null,
    val admissionDate: LocalDate,
    val status: ResidentStatus,
    @JsonProperty("discharged") val isDischarged: Boolean = false,
    val externalId: String? = null
)

data class CreateAssignmentRequest(
    @JsonProperty("bedId") val bedId: String
)

data class AssignmentResponse(
    val id: String,
    @JsonProperty("residentId") val residentId: String,
    @JsonProperty("bedId") val bedId: String,
    val startsAt: Instant,
    val endsAt: Instant? = null,
    @JsonProperty("open") val isOpen: Boolean = false
)

data class DischargeRequest(
    val actorId: String? = null
)

enum class ResidentStatus { ACTIVE, DISCHARGED }
