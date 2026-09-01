package com.hub.coverage.domain.model

import com.hub.shared.domain.WingId
import java.time.Instant

data class UnitShiftCoverage(
    val id: String,
    val wingId: WingId,
    val staffGroupId: StaffGroupId,
    val shiftKey: String,
    val validFrom: Instant,
    val validTo: Instant?
)
