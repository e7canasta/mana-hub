package com.hub.observation.application.service

import com.hub.observation.application.dto.BedStateResponse
import com.hub.observation.application.dto.CurrentStateResponse
import com.hub.observation.domain.model.CurrentBedState
import com.hub.observation.domain.repository.CurrentBedStateRepository
import com.hub.shared.domain.BedId
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.port.BedAssignmentPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BedStateService(
    private val bedStateRepository: CurrentBedStateRepository,
    private val bedAssignmentPort: BedAssignmentPort,
) {

    @Transactional(readOnly = true)
    fun getBedState(bedId: String): BedStateResponse? =
        bedStateRepository.findByBedId(BedId(bedId))?.toResponse()

    @Transactional(readOnly = true)
    fun getCurrentState(residentId: String): CurrentStateResponse {
        val rid = ResidentId(residentId)
        val assignment = bedAssignmentPort.findOpenByResidentId(rid)
            ?: return CurrentStateResponse(residentId = residentId)

        val bedState = bedStateRepository.findByBedId(assignment.bedId)
            ?: return CurrentStateResponse(residentId = residentId, bedId = assignment.bedId.value)

        return CurrentStateResponse(
            residentId = residentId,
            bedId = bedState.bedId.value,
            roomState = bedState.roomState,
            state = bedState.state,
            sleeping = bedState.sleeping,
            stateSince = bedState.stateSince,
            staffPresent = bedState.staffPresent,
        )
    }

    private fun CurrentBedState.toResponse() = BedStateResponse(
        bedId = bedId.value, residentId = residentId?.value, roomState = roomState,
        state = state, sleeping = sleeping, stateSince = stateSince, staffPresent = staffPresent,
    )
}
