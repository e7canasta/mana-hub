package com.hub.shared.domain.port

import com.hub.shared.domain.BedId
import com.hub.shared.domain.ResidentId

interface BedAssignmentPort {
    fun findOpenByBedId(bedId: BedId): BedAssignmentPortModel?
    fun findOpenByResidentId(residentId: ResidentId): BedAssignmentPortModel?
}

data class BedAssignmentPortModel(val residentId: ResidentId, val bedId: BedId)
