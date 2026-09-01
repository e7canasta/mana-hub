package com.hub.observation.infrastructure.adapter

import com.hub.observation.domain.repository.SceneEventRepository
import com.hub.observation.domain.repository.SentinelSignalRepository
import com.hub.shared.domain.BedId
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.port.ObservationQueryPort
import com.hub.shared.domain.port.SceneEventSnapshot
import com.hub.shared.domain.port.SentinelSignalSnapshot
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ObservationQueryAdapter(
    private val sceneEventRepo: SceneEventRepository,
    private val sentinelSignalRepo: SentinelSignalRepository,
) : ObservationQueryPort {

    override fun findScenesByResidentId(residentId: ResidentId, from: Instant?, to: Instant?): List<SceneEventSnapshot> {
        val scenes = if (from != null && to != null) {
            sceneEventRepo.findByResidentId(residentId, from, to)
        } else {
            sceneEventRepo.findByResidentId(residentId)
        }
        return scenes.map { it.toSnapshot() }
    }

    override fun findScenesByBedId(bedId: String): List<SceneEventSnapshot> {
        return sceneEventRepo.findByBedId(BedId(bedId)).map { it.toSnapshot() }
    }

    override fun findSignalsByEpisodeId(episodeId: String): List<SentinelSignalSnapshot> {
        return sentinelSignalRepo.findByEpisodeId(episodeId).map { it.toSnapshot() }
    }

    override fun findSignalsByResidentId(residentId: ResidentId, from: Instant?, to: Instant?): List<SentinelSignalSnapshot> {
        return sentinelSignalRepo.findByResidentId(residentId).map { it.toSnapshot() }
    }

    private fun com.hub.observation.domain.model.SceneEvent.toSnapshot() = SceneEventSnapshot(
        id = id.value,
        bedId = bedId.value,
        residentId = residentId?.value,
        fromState = fromState?.name,
        toState = toState?.name,
        eventType = eventType?.name,
        observedAt = timestamp,
        confidence = null,
        payloadJson = payloadJson,
    )

    private fun com.hub.observation.domain.model.SentinelSignal.toSnapshot() = SentinelSignalSnapshot(
        id = id.value,
        bedId = bedId.value,
        residentId = residentId?.value,
        episodeId = episodeId,
        signalType = type?.name,
        severity = severity,
        observedAt = timestamp,
        trigger = trigger,
        cause = cause,
        state = state,
        triggerOn = triggerOn,
        payloadJson = payloadJson,
    )
}
