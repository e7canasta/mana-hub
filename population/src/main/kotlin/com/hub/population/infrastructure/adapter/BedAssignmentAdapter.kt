package com.hub.population.infrastructure.adapter

import com.hub.shared.domain.port.BedAssignmentPort
import com.hub.shared.domain.port.BedAssignmentPortModel
import com.hub.population.domain.repository.BedAssignmentRepository
import com.hub.shared.domain.BedId
import com.hub.shared.domain.ResidentId
import org.springframework.stereotype.Component

@Component
class BedAssignmentAdapter(private val repo: BedAssignmentRepository) : BedAssignmentPort {
    override fun findOpenByBedId(bedId: BedId): BedAssignmentPortModel? =
        repo.findOpenByBedId(bedId)?.let { BedAssignmentPortModel(residentId = it.residentId, bedId = it.bedId) }

    override fun findOpenByResidentId(residentId: ResidentId): BedAssignmentPortModel? =
        repo.findOpenByResidentId(residentId)?.let { BedAssignmentPortModel(residentId = it.residentId, bedId = it.bedId) }
}
