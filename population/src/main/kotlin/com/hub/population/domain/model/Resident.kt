package com.hub.population.domain.model

import com.hub.shared.domain.AggregateRoot
import com.hub.shared.domain.ResidentId
import com.hub.population.domain.event.ResidentEvent
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
        val next = reconstitute(
            id = id, externalId = externalId, fullName = fullName, birthDate = birthDate,
            admissionDate = admissionDate, status = ResidentStatus.DISCHARGED,
            dischargedAt = Instant.now(), dischargedBy = actorId, version = version + 1
        )
        next._domainEvents.add(
            ResidentEvent.Discharged(residentId = id, actorId = actorId)
        )
        return next
    }

    fun updateProfile(fullName: String?, birthDate: LocalDate?): Resident {
        val next = reconstitute(
            id = id, externalId = externalId, fullName = fullName ?: this.fullName,
            birthDate = birthDate ?: this.birthDate, admissionDate = admissionDate,
            status = status, dischargedAt = dischargedAt, dischargedBy = dischargedBy,
            version = version + 1
        )
        next._domainEvents.add(
            ResidentEvent.Updated(residentId = id, fullName = next.fullName, birthDate = next.birthDate)
        )
        return next
    }

    companion object {
        fun create(
            fullName: String,
            admissionDate: LocalDate,
            birthDate: LocalDate? = null,
            externalId: String? = null
        ): Resident {
            val resident = Resident(
                id = ResidentId.random(), externalId = externalId, fullName = fullName,
                birthDate = birthDate, admissionDate = admissionDate, status = ResidentStatus.ACTIVE,
                dischargedAt = null, dischargedBy = null, version = 0
            )
            resident._domainEvents.add(
                ResidentEvent.Admitted(
                    residentId = resident.id, fullName = fullName,
                    admissionDate = admissionDate, birthDate = birthDate, externalId = externalId,
                )
            )
            return resident
        }

        fun reconstitute(
            id: ResidentId, externalId: String?, fullName: String, birthDate: LocalDate?,
            admissionDate: LocalDate, status: ResidentStatus, dischargedAt: Instant?,
            dischargedBy: String?, version: Long
        ): Resident = Resident(id, externalId, fullName, birthDate, admissionDate, status, dischargedAt, dischargedBy, version)
    }
}
