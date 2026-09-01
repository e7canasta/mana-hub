package com.hub.population.application.service

import com.hub.population.application.dto.*
import com.hub.population.domain.model.*
import com.hub.population.domain.repository.BedAssignmentRepository
import com.hub.population.domain.repository.ResidentRepository
import com.hub.shared.domain.BedId
import com.hub.shared.domain.BedLocation
import com.hub.shared.domain.DomainEventPublisher
import com.hub.shared.domain.LocationResolver
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.publishAndClear
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ResidentApplicationService(
    private val residentRepository: ResidentRepository,
    private val assignmentRepository: BedAssignmentRepository,
    private val locationResolver: LocationResolver,
    private val eventPublisher: DomainEventPublisher
) {

    @Transactional
    fun createResident(request: CreateResidentRequest): ResidentResponse {
        if (request.externalId != null) {
            require(!residentRepository.existsByExternalId(request.externalId)) {
                "External ID already exists: ${request.externalId}"
            }
        }
        val resident = Resident.create(
            fullName = request.fullName,
            admissionDate = request.admissionDate,
            birthDate = request.birthDate,
            externalId = request.externalId
        )
        val saved = residentRepository.save(resident)
        eventPublisher.publishAndClear(resident)
        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    fun getResident(id: ResidentId): ResidentResponse {
        val resident = residentRepository.findById(id)
            ?: throw IllegalArgumentException("Resident not found: $id")
        val assignment = assignmentRepository.findOpenByResidentId(id)
        val location = assignment?.let { locationResolver.resolve(it.bedId) }
        return resident.toResponse(location)
    }

    @Transactional(readOnly = true)
    fun listResidents(): List<ResidentResponse> {
        val residents = residentRepository.findAll()
        val openAssignments = assignmentRepository.findAllOpen().associateBy { it.residentId }
        val bedIds = openAssignments.values.map { it.bedId }.toSet()
        val locations = locationResolver.resolveAll(bedIds)

        return residents.map { resident ->
            val assignment = openAssignments[resident.id]
            val location = assignment?.let { locations[it.bedId] }
            resident.toResponse(location)
        }
    }

    @Transactional
    fun updateResident(id: ResidentId, request: UpdateResidentRequest): ResidentResponse {
        val resident = residentRepository.findById(id)
            ?: throw IllegalArgumentException("Resident not found: $id")
        val updated = resident.updateProfile(request.fullName, request.birthDate)
        val saved = residentRepository.save(updated)
        eventPublisher.publishAndClear(updated)
        return saved.toResponse()
    }

    @Transactional
    fun dischargeResident(id: ResidentId, actorId: String?): ResidentResponse {
        val resident = residentRepository.findById(id)
            ?: throw IllegalArgumentException("Resident not found: $id")
        val discharged = resident.discharge(actorId ?: "system")
        val saved = residentRepository.save(discharged)
        eventPublisher.publishAndClear(discharged)
        return saved.toResponse()
    }

    @Transactional
    fun createAssignment(residentId: ResidentId, request: CreateAssignmentRequest): AssignmentResponse {
        val resident = residentRepository.findById(residentId)
            ?: throw IllegalArgumentException("Resident not found: $residentId")
        require(resident.isActive) { "Resident is not active" }

        val bedId = BedId(request.bedId)
        val existingAssignment = assignmentRepository.findOpenByBedId(bedId)
        require(existingAssignment == null) { "Bed is already assigned" }

        val currentOpen = assignmentRepository.findOpenByResidentId(residentId)
        if (currentOpen != null) {
            assignmentRepository.closeAssignment(currentOpen)
        }

        val assignment = BedAssignment.create(residentId = residentId, bedId = bedId, createdBy = null)
        val saved = assignmentRepository.save(assignment)
        eventPublisher.publishAndClear(assignment)
        return saved.toResponse()
    }

    @Transactional
    fun deleteAssignment(bedId: BedId) {
        val assignment = assignmentRepository.findOpenByBedId(bedId)
            ?: throw IllegalArgumentException("No open assignment for bed: $bedId")
        val closed = assignment.close()
        assignmentRepository.closeAssignment(closed)
        eventPublisher.publishAndClear(closed)
    }

    @Transactional(readOnly = true)
    fun getResidentAssignments(residentId: ResidentId): List<AssignmentResponse> {
        return assignmentRepository.findByResidentId(residentId).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun listOpenAssignments(): List<OpenAssignmentResponse> {
        return assignmentRepository.findAllOpen().map { assignment ->
            val resident = residentRepository.findById(assignment.residentId)
                ?: throw IllegalArgumentException("Resident not found: ${assignment.residentId}")
            OpenAssignmentResponse(
                bedId = assignment.bedId.value,
                residentId = assignment.residentId.value,
                residentName = resident.fullName,
                since = assignment.startsAt
            )
        }
    }

    private fun Resident.toResponse(location: BedLocation? = null): ResidentResponse {
        return ResidentResponse(
            id = id.value, externalId = externalId, fullName = fullName, birthDate = birthDate,
            admissionDate = admissionDate, status = status, isDischarged = !isActive,
            location = location
        )
    }

    private fun BedAssignment.toResponse() = AssignmentResponse(
        id = id.value, residentId = residentId.value, bedId = bedId.value,
        startsAt = startsAt, endsAt = endsAt, isOpen = isOpen
    )
}
