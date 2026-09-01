package com.hub.population.infrastructure.adapter

import com.hub.shared.domain.port.BedAssignmentPort
import com.hub.shared.domain.port.BedAssignmentPortModel
import com.hub.population.domain.repository.BedAssignmentRepository
import com.hub.shared.domain.BedId
import org.springframework.stereotype.Component

@Component
class BedAssignmentAdapter(private val repo: BedAssignmentRepository) : BedAssignmentPort {
    override fun findOpenByBedId(bedId: BedId): BedAssignmentPortModel? {
        return repo.findOpenByBedId(bedId)?.let { BedAssignmentPortModel(residentId = it.residentId) }
    }
}
