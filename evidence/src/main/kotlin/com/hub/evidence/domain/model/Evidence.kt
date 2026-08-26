package com.hub.evidence.domain.model

import com.hub.shared.domain.AggregateRoot
import com.hub.population.domain.model.ResidentId
import com.hub.residence.domain.model.BedId
import java.time.Instant

class Evidence private constructor(
    override val id: EvidenceId,
    val bedId: BedId,
    val residentId: ResidentId,
    val evidenceType: String,
    val category: String?,
    val sceneEventId: String?,
    val sceneEventJson: String?,
    val ruleId: String?,
    val shift: String?,
    val riskLevel: String?,
    val timestamp: Instant,
    override var version: Long
) : AggregateRoot<EvidenceId>() {

    companion object {
        fun create(
            bedId: BedId, residentId: ResidentId, evidenceType: String, category: String?,
            timestamp: Instant
        ): Evidence = Evidence(
            id = EvidenceId.random(), bedId = bedId, residentId = residentId, evidenceType = evidenceType,
            category = category, sceneEventId = null, sceneEventJson = null, ruleId = null,
            shift = null, riskLevel = null, timestamp = timestamp, version = 0
        )

        fun reconstitute(
            id: EvidenceId, bedId: BedId, residentId: ResidentId, evidenceType: String,
            category: String?, sceneEventId: String?, sceneEventJson: String?, ruleId: String?,
            shift: String?, riskLevel: String?, timestamp: Instant, version: Long
        ): Evidence = Evidence(id, bedId, residentId, evidenceType, category, sceneEventId, sceneEventJson, ruleId, shift, riskLevel, timestamp, version)
    }
}
