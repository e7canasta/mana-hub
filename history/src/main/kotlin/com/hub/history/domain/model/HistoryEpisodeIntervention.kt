package com.hub.history.domain.model

import com.hub.shared.domain.Entity
import com.hub.shared.domain.StaffMemberId
import java.time.Instant

class HistoryEpisodeIntervention private constructor(
    override val id: InterventionId,
    val episodeId: HistoryEpisodeId,
    val kind: InterventionKind,
    val performedAt: Instant,
    val performedBy: StaffMemberId?,
    val detail: String?
) : Entity<InterventionId>() {

    companion object {
        fun create(
            episodeId: HistoryEpisodeId,
            kind: InterventionKind,
            performedAt: Instant,
            performedBy: StaffMemberId? = null,
            detail: String? = null
        ): HistoryEpisodeIntervention = HistoryEpisodeIntervention(
            id = InterventionId.random(),
            episodeId = episodeId,
            kind = kind,
            performedAt = performedAt,
            performedBy = performedBy,
            detail = detail
        )

        fun reconstitute(
            id: InterventionId,
            episodeId: HistoryEpisodeId,
            kind: InterventionKind,
            performedAt: Instant,
            performedBy: StaffMemberId?,
            detail: String?
        ): HistoryEpisodeIntervention = HistoryEpisodeIntervention(
            id, episodeId, kind, performedAt, performedBy, detail
        )
    }
}
