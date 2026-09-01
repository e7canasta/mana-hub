package com.hub.views.readers

import com.hub.population.domain.repository.BedAssignmentRepository
import com.hub.population.domain.repository.ResidentRepository
import com.hub.observation.domain.repository.CurrentBedStateRepository
import com.hub.shared.domain.BedLocation
import com.hub.shared.domain.LocationResolver
import com.hub.shared.domain.ResidentId
import com.hub.views.RailState
import com.hub.views.ResidentChartProjection
import com.hub.views.ResidentRailItem
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ResidentProjectionReader(
    private val residentRepository: ResidentRepository,
    private val bedAssignmentRepository: BedAssignmentRepository,
    private val bedStateRepository: CurrentBedStateRepository,
    private val locationResolver: LocationResolver,
) {

    private fun BedLocation?.toRailLocation(): BedLocation? = this

    @Transactional(readOnly = true)
    fun getResidentRail(): List<ResidentRailItem> {
        val residents = residentRepository.findAll()
        return residents.map { resident ->
            val assignment = bedAssignmentRepository.findOpenByResidentId(resident.id)
            val bedState = assignment?.let { bedStateRepository.findByBedId(it.bedId) }
            ResidentRailItem(
                id = resident.id.value,
                fullName = resident.fullName,
                location = assignment?.let { locationResolver.resolve(it.bedId)?.toRailLocation() },
                    currentState = bedState?.let {
                    RailState(
                        state = it.state,
                        staffPresent = it.staffPresent,
                        stateSince = it.stateSince,
                    )
                },
            )
        }
    }

    @Transactional(readOnly = true)
    fun getResidentChart(residentId: String): ResidentChartProjection? {
        val resident = residentRepository.findById(ResidentId(residentId)) ?: return null
        val assignment = bedAssignmentRepository.findOpenByResidentId(ResidentId(residentId))
        val bedState = assignment?.let { bedStateRepository.findByBedId(it.bedId) }
        return ResidentChartProjection(
            id = resident.id.value,
            fullName = resident.fullName,
            birthDate = resident.birthDate,
            admissionDate = resident.admissionDate,
            location = assignment?.let { locationResolver.resolve(it.bedId)?.toRailLocation() },
            currentState = bedState?.let {
                RailState(state = it.state, staffPresent = it.staffPresent, stateSince = it.stateSince)
            },
        )
    }
}
