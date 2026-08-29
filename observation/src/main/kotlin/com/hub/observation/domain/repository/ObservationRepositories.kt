package com.hub.observation.domain.repository

import com.hub.observation.domain.model.*
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.BedId
import java.time.Instant
import java.time.LocalDate

interface SensorEventRepository {
    fun findByMonitorKey(monitorKey: String): List<SensorEvent>
    fun findUnresolved(): List<SensorEvent>
    fun save(event: SensorEvent): SensorEvent
}

interface CurrentBedStateRepository {
    fun findByBedId(bedId: BedId): CurrentBedState?
    fun findAll(): List<CurrentBedState>
    fun save(state: CurrentBedState): CurrentBedState
}

interface SummaryRepository {
    fun findSleepByResidentAndDate(residentId: ResidentId, date: LocalDate): SleepSummary?
    fun findSleepByResidentAndRange(residentId: ResidentId, from: LocalDate, to: LocalDate): List<SleepSummary>
    fun findMobilityByResidentAndDate(residentId: ResidentId, date: LocalDate): MobilitySummary?
    fun findMobilityByResidentAndRange(residentId: ResidentId, from: LocalDate, to: LocalDate): List<MobilitySummary>
    fun findBathroomByResidentAndDate(residentId: ResidentId, date: LocalDate): BathroomSummary?
    fun findBathroomByResidentAndRange(residentId: ResidentId, from: LocalDate, to: LocalDate): List<BathroomSummary>
    fun saveSleep(summary: SleepSummary): SleepSummary
    fun saveMobility(summary: MobilitySummary): MobilitySummary
    fun saveBathroom(summary: BathroomSummary): BathroomSummary
}

interface NotificationEventRepository {
    fun findByResidentId(residentId: ResidentId): List<NotificationEvent>
    fun findByBedId(bedId: BedId): List<NotificationEvent>
    fun save(event: NotificationEvent): NotificationEvent
}
