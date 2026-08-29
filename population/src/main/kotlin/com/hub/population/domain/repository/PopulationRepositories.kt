package com.hub.population.domain.repository

import com.hub.population.domain.model.*
import com.hub.shared.domain.BedId
import com.hub.shared.domain.ResidentId

interface ResidentRepository {
    fun findById(id: ResidentId): Resident?
    fun findByExternalId(externalId: String): Resident?
    fun findAll(): List<Resident>
    fun save(resident: Resident): Resident
    fun existsByExternalId(externalId: String): Boolean
}

interface BedAssignmentRepository {
    fun findById(id: AssignmentId): BedAssignment?
    fun findByResidentId(residentId: ResidentId): List<BedAssignment>
    fun findAllOpen(): List<BedAssignment>
    fun findByBedId(bedId: BedId): BedAssignment?
    fun findOpenByResidentId(residentId: ResidentId): BedAssignment?
    fun findOpenByBedId(bedId: BedId): BedAssignment?
    fun save(assignment: BedAssignment): BedAssignment
    fun closeAssignment(assignment: BedAssignment)
}
