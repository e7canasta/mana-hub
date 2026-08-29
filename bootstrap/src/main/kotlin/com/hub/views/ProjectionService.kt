package com.hub.views

import com.hub.population.domain.repository.BedAssignmentRepository
import com.hub.population.domain.repository.ResidentRepository
import com.hub.observation.domain.repository.CurrentBedStateRepository
import com.hub.observation.domain.repository.SummaryRepository
import com.hub.history.domain.repository.HistoryEpisodeDetectionRepository
import com.hub.history.domain.repository.HistoryEpisodeReviewRepository
import com.hub.care.domain.repository.CareSummaryRepository
import com.hub.policy.domain.repository.AlarmProfileRepository
import com.hub.policy.domain.repository.AlarmProfileOverrideRepository
import com.hub.shared.domain.ResidentId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

@Service
class ProjectionService(
    private val residentRepository: ResidentRepository,
    private val bedAssignmentRepository: BedAssignmentRepository,
    private val bedStateRepository: CurrentBedStateRepository,
    private val summaryRepository: SummaryRepository,
    private val historyEpisodeRepository: HistoryEpisodeDetectionRepository,
    private val historyReviewRepository: HistoryEpisodeReviewRepository,
    private val careSummaryRepository: CareSummaryRepository,
    private val alarmProfileRepository: AlarmProfileRepository,
    private val alarmOverrideRepository: AlarmProfileOverrideRepository,
) {

    // ──────────────────────────────────────────────────────── resident-rail

    @Transactional(readOnly = true)
    fun getResidentRail(): List<ResidentRailItem> {
        val residents = residentRepository.findAll()
        return residents.map { resident ->
            val assignment = bedAssignmentRepository.findOpenByResidentId(resident.id)
            val bedState = assignment?.let { bedStateRepository.findByBedId(it.bedId) }
            ResidentRailItem(
                id = resident.id.value,
                fullName = resident.fullName,
                location = assignment?.let {
                    RailLocation(wingName = null, roomNumber = null, bedLabel = null)
                },
                currentState = bedState?.let {
                    RailState(
                        state = it.state,
                        staffPresent = null,
                        stateSince = it.stateSince,
                    )
                },
            )
        }
    }

    // ──────────────────────────────────────────────────── resident-chart

    @Transactional(readOnly = true)
    fun getResidentChart(residentId: String): ResidentChartProjection? {
        val resident = residentRepository.findById(ResidentId(residentId)) ?: return null
        val assignment = bedAssignmentRepository.findOpenByResidentId(ResidentId(residentId))
        val bedState = assignment?.let { bedStateRepository.findByBedId(it.bedId) }
        return ResidentChartProjection(
            id = resident.id.value,
            fullName = resident.fullName,
            birthDate = resident.birthDate,
            admissionDate = resident.admissionDate,
            location = assignment?.let {
                RailLocation(wingName = null, roomNumber = null, bedLabel = null)
            },
            currentState = bedState?.let {
                RailState(state = it.state, staffPresent = null, stateSince = it.stateSince)
            },
        )
    }

    // ──────────────────────────────────────────────────────── sleep

    @Transactional(readOnly = true)
    fun getSleepTab(residentId: String, from: LocalDate, to: LocalDate): SleepTabProjection {
        val summaries = summaryRepository.findSleepByResidentAndRange(
            ResidentId(residentId), from, to
        )
        return SleepTabProjection(
            residentId = residentId,
            from = from.toString(),
            to = to.toString(),
            summaries = summaries.map {
                SleepDayProjection(
                    day = it.observedOn.toString(),
                    calmMinutes = it.calmMinutes,
                    restlessMinutes = it.restlessMinutes,
                    awakeMinutes = it.awakeMinutes,
                    outOfBedMinutes = it.outOfBedMinutes,
                    bedExitCount = it.bedExitCount,
                    wakeCount = it.wakeCount,
                    startedAt = it.startedAt,
                    endedAt = it.endedAt,
                )
            },
        )
    }

    // ──────────────────────────────────────────────────── mobility

    @Transactional(readOnly = true)
    fun getMobilityTab(residentId: String, from: LocalDate, to: LocalDate): MobilityTabProjection {
        val summaries = summaryRepository.findMobilityByResidentAndRange(
            ResidentId(residentId), from, to
        )
        return MobilityTabProjection(
            residentId = residentId,
            from = from.toString(),
            to = to.toString(),
            summaries = summaries.map {
                MobilityDayProjection(
                    day = it.observedOn.toString(),
                    walkingMinutes = it.walkingMinutes,
                    distanceMeters = it.distanceMeters,
                    transferCount = it.transferCount,
                    outOfBedMinutes = it.outOfBedMinutes,
                )
            },
        )
    }

    // ──────────────────────────────────────────────────── bathroom

    @Transactional(readOnly = true)
    fun getBathroomTab(residentId: String, from: LocalDate, to: LocalDate): BathroomTabProjection {
        val summaries = summaryRepository.findBathroomByResidentAndRange(
            ResidentId(residentId), from, to
        )
        return BathroomTabProjection(
            residentId = residentId,
            from = from.toString(),
            to = to.toString(),
            summaries = summaries.map {
                BathroomDayProjection(
                    day = it.observedOn.toString(),
                    visitCount = it.visitCount,
                    nightVisitCount = it.nightVisitCount,
                )
            },
        )
    }

    // ──────────────────────────────────────────────────────── care

    @Transactional(readOnly = true)
    fun getCareTab(residentId: String, from: LocalDate, to: LocalDate): CareTabProjection {
        val summaries = careSummaryRepository.findByResidentAndRange(
            ResidentId(residentId), from, to
        )
        val days = summaries.map {
            CareDayProjection(
                day = it.observedOn.toString(),
                totalMinutes = it.totalMinutes,
                proactiveMinutes = it.proactiveMinutes,
                roundsCount = it.roundsCount,
                notesCount = it.notesCount,
            )
        }
        val total = days.sumOf { it.totalMinutes }
        val proactive = days.sumOf { it.proactiveMinutes }
        return CareTabProjection(
            residentId = residentId,
            from = from.toString(),
            to = to.toString(),
            summaries = days,
            avgMinutesPerDay = if (days.isEmpty()) null else total.toDouble() / days.size,
            proactiveShare = if (total == 0) null else proactive.toDouble() / total,
        )
    }

    // ──────────────────────────────────────────────────────── falls

    @Transactional(readOnly = true)
    fun getFallsTab(residentId: String, months: Int = 12): FallsTabProjection {
        val episodes = historyEpisodeRepository.findByResidentId(ResidentId(residentId))
        val falls = episodes.filter { it.kind.name == "FALL" }
            .sortedByDescending { it.occurredAt }

        val now = LocalDate.now()
        val monthRange = (0 until months).map { YearMonth.now().minusMonths(it.toLong()) }

        val lastFall = falls.firstOrNull()
        val lastFallAt = lastFall?.occurredAt
        val streakDays = if (lastFallAt != null) {
            java.time.temporal.ChronoUnit.DAYS.between(
                lastFallAt.atZone(ZoneOffset.UTC).toLocalDate(), now
            ).toInt().coerceAtLeast(0)
        } else {
            now.toEpochDay().toInt()
        }

        val previousFall = falls.drop(1).firstOrNull()
        val previousStreakDays = if (previousFall != null && lastFallAt != null) {
            java.time.temporal.ChronoUnit.DAYS.between(
                previousFall.occurredAt.atZone(ZoneOffset.UTC).toLocalDate(),
                lastFallAt.atZone(ZoneOffset.UTC).toLocalDate()
            ).toInt().coerceAtLeast(0)
        } else 0

        return FallsTabProjection(
            residentId = residentId,
            streakDays = streakDays,
            previousStreakDays = previousStreakDays,
            fallsLast12Months = falls.size,
            lastFallAt = lastFallAt,
            lastFallInjury = lastFall?.injuryStatus,
            months = monthRange.map { ym ->
                FallMonthProjection(
                    label = ym.toString(),
                    falls = falls.count {
                        it.occurredAt.atZone(ZoneOffset.UTC).toLocalDate().yearMonth == ym
                    },
                )
            },
        )
    }

    private val LocalDate.yearMonth: YearMonth get() = YearMonth.from(this)

    // ─────────────────────────────────────────────────────── episodes

    @Transactional(readOnly = true)
    fun getEpisodesTab(residentId: String): EpisodesTabProjection {
        val episodes = historyEpisodeRepository.findByResidentId(ResidentId(residentId))
        val reviews = episodes.map { ep ->
            historyReviewRepository.findByEpisodeId(ep.id)
        }
        return EpisodesTabProjection(
            residentId = residentId,
            episodes = episodes.zip(reviews).map { (ep, revs) ->
                val lastReview = revs.maxByOrNull { it.resolvedAt ?: java.time.Instant.MIN }
                EpisodeListItemProjection(
                    id = ep.id.value,
                    kind = ep.kind.name,
                    severity = ep.severity.name,
                    occurredAt = ep.occurredAt,
                    injuryStatus = ep.injuryStatus,
                    selfRecovery = ep.selfRecovery,
                    verdict = lastReview?.detectionVerdict,
                    reviewNote = lastReview?.reviewNote,
                    reviewedAt = lastReview?.resolvedAt,
                )
            },
        )
    }

    // ─────────────────────────────────────────────────────── alarm (read)

    @Transactional(readOnly = true)
    fun getAlarmPresets(residentId: String): AlarmPresetsProjection {
        val version = alarmProfileRepository.findCurrentByResidentId(ResidentId(residentId))
        if (version == null) {
            return AlarmPresetsProjection(
                residentId = residentId,
                riskLevel = null, mobilityAid = null, autopilot = null,
                mode = null, templateId = null, overrides = emptyMap(),
                updatedAt = null, updatedBy = null, recommendation = null,
            )
        }
        val overrides = alarmOverrideRepository.findByProfileVersionId(version.id.value)
            .associate { override ->
                override.ruleId to when (override) {
                    is com.hub.policy.domain.model.PolicyOverride.DwellOverride ->
                        mapOf("warningAfterMinutes" to override.warningAfterMinutes, "alertAfterMinutes" to override.alertAfterMinutes)
                    is com.hub.policy.domain.model.PolicyOverride.HysteresisOverride ->
                        mapOf("hysteresisSeconds" to override.hysteresisSeconds)
                    is com.hub.policy.domain.model.PolicyOverride.ComeBackOverride ->
                        mapOf("baselineState" to override.baselineState, "alertAfterMinutes" to override.alertAfterMinutes)
                }
            }
        return AlarmPresetsProjection(
            residentId = residentId,
            riskLevel = version.riskLevel.name.lowercase(),
            mobilityAid = version.mobilityAid?.name?.lowercase(),
            autopilot = version.autopilot,
            mode = version.mode?.name?.lowercase(),
            templateId = version.templateId?.value,
            overrides = overrides,
            updatedAt = version.validFrom.toString(),
            updatedBy = version.updatedBy,
            recommendation = null,
        )
    }
}
