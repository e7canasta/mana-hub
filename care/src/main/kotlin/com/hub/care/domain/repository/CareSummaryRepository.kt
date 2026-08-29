package com.hub.care.domain.repository

import com.hub.care.domain.model.CareSummary
import com.hub.shared.domain.ResidentId
import java.time.LocalDate

interface CareSummaryRepository {
    fun findByResidentAndDate(residentId: ResidentId, date: LocalDate): CareSummary?
    fun findByResidentAndRange(residentId: ResidentId, from: LocalDate, to: LocalDate): List<CareSummary>
    fun save(summary: CareSummary): CareSummary
}
