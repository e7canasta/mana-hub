package com.hub.population.application.service

import com.hub.population.application.dto.*
import com.hub.population.domain.model.*
import com.hub.population.domain.repository.BedAssignmentRepository
import com.hub.population.domain.repository.ResidentRepository
import com.hub.residence.domain.model.BedId
import com.hub.shared.domain.DomainEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ResidentApplicationService(
    private val residentRepository: ResidentRepository,
    private val assignmentRepository: BedAssignmentRepository,
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
        return residentRepository.save(resident).toResponse()
    }

    @Transactional(readOnly = true)
    fun getResident(id: ResidentId): ResidentResponse {
        return residentRepository.findById(id)?.toResponse()
            ?: throw IllegalArgumentException("Resident not found: $id")
    }

    @Transactional(readOnly = true)
    fun listResidents(): List<ResidentResponse> {
        return residentRepository.findAll().map { it.toResponse() }
    }

    @Transactional
    fun updateResident(id: ResidentId, request: UpdateResidentRequest): ResidentResponse {
        val resident = residentRepository.findById(id)
            ?: throw IllegalArgumentException("Resident not found: $id")
        return residentRepository.save(resident.updateProfile(request.fullName, request.birthDate)).toResponse()
    }

    @Transactional
    fun dischargeResident(id: ResidentId, actorId: String?): ResidentResponse {
        val resident = residentRepository.findById(id)
            ?: throw IllegalArgumentException("Resident not found: $id")
        val discharged = resident.discharge(actorId ?: "system")
        return residentRepository.save(discharged).toResponse()
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
        return assignmentRepository.save(assignment).toResponse()
    }

    @Transactional
    fun deleteAssignment(bedId: BedId) {
        val assignment = assignmentRepository.findOpenByBedId(bedId)
            ?: throw IllegalArgumentException("No open assignment for bed: $bedId")
        assignmentRepository.closeAssignment(assignment)
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

    private fun Resident.toResponse() = ResidentResponse(
        id = id.value, externalId = externalId, fullName = fullName, birthDate = birthDate,
        admissionDate = admissionDate, status = status, isDischarged = !isActive
    )

    private fun BedAssignment.toResponse() = AssignmentResponse(
        id = id.value, residentId = residentId.value, bedId = bedId.value,
        startsAt = startsAt, endsAt = endsAt, isOpen = isOpen
    )
}
