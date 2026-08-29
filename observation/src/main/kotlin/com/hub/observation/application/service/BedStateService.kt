package com.hub.observation.application.service

import com.hub.observation.application.dto.BedStateResponse
import com.hub.observation.application.dto.CurrentStateResponse
import com.hub.observation.domain.model.CurrentBedState
import com.hub.observation.domain.repository.CurrentBedStateRepository
import com.hub.population.domain.repository.BedAssignmentRepository
import com.hub.shared.domain.BedId
import com.hub.shared.domain.ResidentId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BedStateService(
    private val bedStateRepository: CurrentBedStateRepository,
    private val bedAssignmentRepository: BedAssignmentRepository
) {

    @Transactional(readOnly = true)
    fun getBedState(bedId: String): BedStateResponse? {
        return bedStateRepository.findByBedId(BedId(bedId))?.toResponse()
    }

    @Transactional(readOnly = true)
    fun getCurrentState(residentId: String): CurrentStateResponse {
        val assignment = bedAssignmentRepository.findOpenByResidentId(ResidentId(residentId))
        if (assignment == null) {
            return CurrentStateResponse(
                residentId = residentId,
                bedId = null,
                roomState = null,
                state = null,
                sleeping = null,
                stateSince = null,
                staffPresent = null
            )
        }
        val bedState = bedStateRepository.findByBedId(assignment.bedId)
        if (bedState == null) {
            return CurrentStateResponse(
                residentId = residentId,
                bedId = assignment.bedId.value,
                roomState = null,
                state = null,
                sleeping = null,
                stateSince = null,
                staffPresent = null
            )
        }
        return CurrentStateResponse(
            residentId = residentId,
            bedId = bedState.bedId.value,
            roomState = bedState.roomState,
            state = bedState.state,
            sleeping = bedState.sleeping,
            stateSince = bedState.stateSince,
            staffPresent = bedState.staffPresent
        )
    }

    private fun CurrentBedState.toResponse(): BedStateResponse {
        return BedStateResponse(
            bedId = bedId.value, residentId = residentId?.value, roomState = roomState,
            state = state, sleeping = sleeping, stateSince = stateSince, staffPresent = staffPresent
        )
    }
}
