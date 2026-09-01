package com.hub.integration.port

import com.hub.shared.domain.BedId
import com.hub.shared.domain.ResidentId

interface BedAssignmentPort {
    fun findOpenByBedId(bedId: BedId): BedAssignmentPortModel?
}

data class BedAssignmentPortModel(val residentId: ResidentId)
