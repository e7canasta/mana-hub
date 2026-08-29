package com.hub.population.application.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.hub.population.domain.model.ResidentStatus
import java.time.Instant
import java.time.LocalDate

data class CreateResidentRequest(
    val fullName: String,
    val birthDate: LocalDate? = null,
    val admissionDate: LocalDate,
    val externalId: String? = null
)

data class UpdateResidentRequest(
    val fullName: String? = null,
    val birthDate: LocalDate? = null
)

data class ResidentLocation(
    val wingId: String?,
    val wingName: String?,
    val roomNumber: String?,
    val bedId: String?,
    val bedLabel: String?
)

data class ResidentResponse(
    val id: String,
    val externalId: String?,
    val fullName: String,
    val birthDate: LocalDate?,
    val admissionDate: LocalDate,
    val status: ResidentStatus,
    @JsonProperty("isDischarged") val isDischarged: Boolean,
    val location: ResidentLocation? = null
)

data class CreateAssignmentRequest(
    val bedId: String
)

data class AssignmentResponse(
    val id: String,
    val residentId: String,
    val bedId: String,
    val startsAt: Instant,
    val endsAt: Instant?,
    @JsonProperty("isOpen") val isOpen: Boolean
)

data class OpenAssignmentResponse(
    val bedId: String,
    val residentId: String,
    val residentName: String,
    val since: Instant
)

data class DischargeRequest(
    val actorId: String? = null
)
