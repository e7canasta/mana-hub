package com.hub.population.domain.model

import com.hub.shared.domain.AggregateRoot
import com.hub.shared.domain.ResidentId
import java.time.Instant
import java.time.LocalDate

enum class ResidentStatus {
    ACTIVE,
    DISCHARGED;

    companion object {
        fun from(value: String): ResidentStatus = when (value.lowercase()) {
            "active" -> ACTIVE
            "discharged" -> DISCHARGED
            else -> throw IllegalArgumentException("Unknown status: $value")
        }
    }
}

class Resident private constructor(
    override val id: ResidentId,
    val externalId: String?,
    val fullName: String,
    val birthDate: LocalDate?,
    val admissionDate: LocalDate,
    val status: ResidentStatus,
    val dischargedAt: Instant?,
    val dischargedBy: String?,
    override var version: Long
) : AggregateRoot<ResidentId>() {

    val isActive: Boolean get() = status == ResidentStatus.ACTIVE

    fun discharge(actorId: String): Resident {
        require(isActive) { "Resident is already discharged" }
        return reconstitute(
            id = id, externalId = externalId, fullName = fullName, birthDate = birthDate,
            admissionDate = admissionDate, status = ResidentStatus.DISCHARGED,
            dischargedAt = Instant.now(), dischargedBy = actorId, version = version + 1
        )
    }

    fun updateProfile(fullName: String?, birthDate: LocalDate?): Resident {
        return reconstitute(
            id = id, externalId = externalId, fullName = fullName ?: this.fullName,
            birthDate = birthDate ?: this.birthDate, admissionDate = admissionDate,
            status = status, dischargedAt = dischargedAt, dischargedBy = dischargedBy,
            version = version + 1
        )
    }

    companion object {
        fun create(
            fullName: String,
            admissionDate: LocalDate,
            birthDate: LocalDate? = null,
            externalId: String? = null
        ): Resident = Resident(
            id = ResidentId.random(), externalId = externalId, fullName = fullName,
            birthDate = birthDate, admissionDate = admissionDate, status = ResidentStatus.ACTIVE,
            dischargedAt = null, dischargedBy = null, version = 0
        )

        fun reconstitute(
            id: ResidentId, externalId: String?, fullName: String, birthDate: LocalDate?,
            admissionDate: LocalDate, status: ResidentStatus, dischargedAt: Instant?,
            dischargedBy: String?, version: Long
        ): Resident = Resident(id, externalId, fullName, birthDate, admissionDate, status, dischargedAt, dischargedBy, version)
    }
}
