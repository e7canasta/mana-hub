package com.hub.views

import com.hub.population.domain.repository.BedAssignmentRepository
import com.hub.population.domain.repository.ResidentRepository
import com.hub.observation.domain.repository.CurrentBedStateRepository
import com.hub.observation.domain.repository.SummaryRepository
import com.hub.history.domain.repository.HistoryEpisodeDetectionRepository
import com.hub.history.domain.repository.HistoryEpisodeReviewRepository
import com.hub.history.domain.model.HistoryEpisodeId
import com.hub.surveillance.domain.repository.EpisodeRepository
import com.hub.care.domain.repository.CareSummaryRepository
import com.hub.policy.domain.repository.AlarmProfileRepository
import com.hub.policy.domain.repository.AlarmProfileOverrideRepository
import com.hub.shared.domain.BedId
import com.hub.shared.domain.BedLocation
import com.hub.shared.domain.LocationResolver
import com.hub.shared.domain.ResidentId
import com.hub.shared.time.DateRange
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Service
class ProjectionService(
    private val residentRepository: ResidentRepository,
    private val bedAssignmentRepository: BedAssignmentRepository,
    private val bedStateRepository: CurrentBedStateRepository,
    private val summaryRepository: SummaryRepository,
    private val historyEpisodeRepository: HistoryEpisodeDetectionRepository,
    private val historyReviewRepository: HistoryEpisodeReviewRepository,
    private val episodeRepository: EpisodeRepository,
    private val careSummaryRepository: CareSummaryRepository,
    private val alarmProfileRepository: AlarmProfileRepository,
    private val alarmOverrideRepository: AlarmProfileOverrideRepository,
    private val locationResolver: LocationResolver,
) {

    private fun BedLocation?.toRailLocation(): BedLocation? = this

    /**
     * Días antes de la admisión no son observables: el cubo/panel no deben
     * mostrar 14 ceros ficticios (Susan día 1–2). Cero = medido; pre-admisión = ausente.
     */
    private fun effectiveObservationFrom(residentId: String, from: LocalDate): LocalDate {
        val admission = residentRepository.findById(ResidentId(residentId))?.admissionDate
        return if (admission != null && admission.isAfter(from)) admission else from
    }

    private fun datesInRange(from: LocalDate, to: LocalDate): List<LocalDate> =
        DateRange.datesBetween(from, to)

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
                location = assignment?.let { locationResolver.resolve(it.bedId)?.toRailLocation() },
                    currentState = bedState?.let {
                    RailState(
                        state = it.state,
                        staffPresent = it.staffPresent,
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
            location = assignment?.let { locationResolver.resolve(it.bedId)?.toRailLocation() },
            currentState = bedState?.let {
                RailState(state = it.state, staffPresent = it.staffPresent, stateSince = it.stateSince)
            },
        )
    }

    // ──────────────────────────────────────────────────────── sleep

    @Transactional(readOnly = true)
    fun getSleepTab(residentId: String, from: LocalDate, to: LocalDate): SleepTabProjection {
        val observedFrom = effectiveObservationFrom(residentId, from)
        val byDay = summaryRepository.findSleepByResidentAndRange(
            ResidentId(residentId), observedFrom, to
        ).associateBy { it.observedOn }
        val days = datesInRange(observedFrom, to).map { date ->
            val summary = byDay[date]
            SleepDayProjection(
                day = date.toString(),
                calmMinutes = summary?.calmMinutes ?: 0,
                restlessMinutes = summary?.restlessMinutes ?: 0,
                awakeMinutes = summary?.awakeMinutes ?: 0,
                outOfBedMinutes = summary?.outOfBedMinutes ?: 0,
                bedExitCount = summary?.bedExitCount ?: 0,
                wakeCount = summary?.wakeCount ?: 0,
                startedAt = summary?.startedAt,
                endedAt = summary?.endedAt,
                measured = summary != null,
            )
        }
        return SleepTabProjection(
            residentId = residentId,
            from = from.toString(),
            to = to.toString(),
            observedFrom = observedFrom.toString(),
            summaries = days,
        )
    }

    // ──────────────────────────────────────────────────── mobility

    @Transactional(readOnly = true)
    fun getMobilityTab(residentId: String, from: LocalDate, to: LocalDate): MobilityTabProjection {
        val observedFrom = effectiveObservationFrom(residentId, from)
        val byDay = summaryRepository.findMobilityByResidentAndRange(
            ResidentId(residentId), observedFrom, to
        ).associateBy { it.observedOn }
        val days = datesInRange(observedFrom, to).map { date ->
            val summary = byDay[date]
            MobilityDayProjection(
                day = date.toString(),
                walkingMinutes = summary?.walkingMinutes ?: 0,
                distanceMeters = summary?.distanceMeters ?: 0.0,
                transferCount = summary?.transferCount ?: 0,
                outOfBedMinutes = summary?.outOfBedMinutes ?: 0,
                inBedMinutes = summary?.inBedMinutes ?: 0,
                outOfSightMinutes = summary?.outOfSightMinutes ?: 0,
                measured = summary != null,
            )
        }
        return MobilityTabProjection(
            residentId = residentId,
            from = from.toString(),
            to = to.toString(),
            observedFrom = observedFrom.toString(),
            summaries = days,
        )
    }

    // ──────────────────────────────────────────────────── bathroom

    @Transactional(readOnly = true)
    fun getBathroomTab(residentId: String, from: LocalDate, to: LocalDate): BathroomTabProjection {
        val observedFrom = effectiveObservationFrom(residentId, from)
        val byDay = summaryRepository.findBathroomByResidentAndRange(
            ResidentId(residentId), observedFrom, to
        ).associateBy { it.observedOn }
        val days = datesInRange(observedFrom, to).map { date ->
            val summary = byDay[date]
            BathroomDayProjection(
                day = date.toString(),
                visitCount = summary?.visitCount ?: 0,
                nightVisitCount = summary?.nightVisitCount ?: 0,
                assistedCount = summary?.assistedCount ?: 0,
                totalMinutes = summary?.totalMinutes ?: 0,
                measured = summary != null,
            )
        }
        return BathroomTabProjection(
            residentId = residentId,
            from = from.toString(),
            to = to.toString(),
            observedFrom = observedFrom.toString(),
            summaries = days,
        )
    }

    // ──────────────────────────────────────────────────────── care

    @Transactional(readOnly = true)
    fun getCareTab(residentId: String, from: LocalDate, to: LocalDate): CareTabProjection {
        val observedFrom = effectiveObservationFrom(residentId, from)
        val byDay = careSummaryRepository.findByResidentAndRange(
            ResidentId(residentId), observedFrom, to
        ).associateBy { it.observedOn }
        val days = datesInRange(observedFrom, to).map { date ->
            val summary = byDay[date]
            CareDayProjection(
                day = date.toString(),
                totalMinutes = summary?.totalMinutes ?: 0,
                proactiveMinutes = summary?.proactiveMinutes ?: 0,
                roundsCount = summary?.roundsCount ?: 0,
                notesCount = summary?.notesCount ?: 0,
                measured = summary != null,
            )
        }
        val measuredDays = days.filter { it.measured }
        val total = measuredDays.sumOf { it.totalMinutes }
        val proactive = measuredDays.sumOf { it.proactiveMinutes }
        val roundsObserved = measuredDays.any { it.roundsCount > 0 }
        return CareTabProjection(
            residentId = residentId,
            from = from.toString(),
            to = to.toString(),
            observedFrom = observedFrom.toString(),
            summaries = days,
            avgMinutesPerDay = if (measuredDays.isEmpty()) null else total.toDouble() / measuredDays.size,
            proactiveShare = when {
                !roundsObserved -> null
                total == 0 -> 0.0
                else -> proactive.toDouble() / total
            },
        )
    }

    // ──────────────────────────────────────────────────────── falls

    @Transactional(readOnly = true)
    fun getFallsTab(residentId: String, months: Int = 12): FallsTabProjection {
        val episodes = historyEpisodeRepository.findByResidentId(ResidentId(residentId))
        val falls = episodes.filter { it.kind.name == "FALL" }
            .sortedByDescending { it.occurredAt }
        val engineEpisodes = episodeRepository.findByResidentId(ResidentId(residentId))
        val reviewedEngineFalls = engineEpisodes
            .filter { engine ->
                val latestReview = historyReviewRepository
                    .findByEpisodeId(HistoryEpisodeId(engine.id.value))
                    .maxByOrNull { it.resolvedAt ?: Instant.MIN }
                latestReview?.detectionVerdict.equals("confirmed", ignoreCase = true)
            }
            .map { it.occurredAt to null as String? }
        val fallFacts = (falls.map { it.occurredAt to it.injuryStatus } + reviewedEngineFalls)
            .distinctBy { it.first }
            .sortedByDescending { it.first }

        val assignment = bedAssignmentRepository.findOpenByResidentId(ResidentId(residentId))
        val zone = assignment?.let { locationResolver.zone(it.bedId) } ?: DEFAULT_ZONE
        val now = LocalDate.now(zone)
        val monthRange = (0 until months).map { YearMonth.now(zone).minusMonths(it.toLong()) }
        val firstMonth = monthRange.last()
        val from = firstMonth.atDay(1)
        val sleepSummaries = summaryRepository.findSleepByResidentAndRange(
            ResidentId(residentId), from, now,
        )

        val fallsInRange = fallFacts.filter {
            it.first.atZone(zone).toLocalDate() >= from &&
                it.first.atZone(zone).toLocalDate() <= now
        }
        val exitsInRange = episodes.filter { episode ->
            episode.kind.name == "BED_EXIT" &&
                episode.occurredAt.atZone(zone).toLocalDate() >= from &&
                episode.occurredAt.atZone(zone).toLocalDate() <= now
        }
        val lastFall = fallFacts.firstOrNull()
        val lastFallAt = lastFall?.first

        /*
         * "34 dias sin caidas" es una afirmacion, y el panel la muestra grande.
         * Solo se puede hacer si hay desde cuando contar.
         *
         * Con una caida registrada, se cuenta desde esa. Sin ninguna, se cuenta
         * desde el primer episodio observado de esa persona: es la fecha desde
         * la que el sistema efectivamente la esta mirando, y por lo tanto el
         * unico piso defendible. Sin ningun episodio no hay observacion y la
         * respuesta es null, no un numero.
         *
         * Antes devolvia now.toEpochDay() -los dias desde 1970-, o sea 20694
         * dias sin caidas para alguien que ingreso el mes pasado.
         */
        val firstEpisodeObservedAt = episodes.minByOrNull { it.occurredAt }?.occurredAt
        val firstSummaryObservedAt = sleepSummaries.minOfOrNull { it.observedOn }
            ?.atStartOfDay(zone)?.toInstant()
        val firstObservedAt = listOfNotNull(firstEpisodeObservedAt, firstSummaryObservedAt).minOrNull()
        val streakFrom = lastFallAt ?: firstObservedAt
        val streakDays = streakFrom?.let {
            ChronoUnit.DAYS.between(it.atZone(zone).toLocalDate(), now).toInt().coerceAtLeast(0)
        }

        val previousFall = fallFacts.drop(1).firstOrNull()
        /* Null y no 0: "no hubo una caida anterior" y "la anterior fue el mismo
         * dia" son cosas distintas, y con 0 se leen igual. */
        val previousStreakDays = if (previousFall != null && lastFallAt != null) {
            ChronoUnit.DAYS.between(
                previousFall.first.atZone(zone).toLocalDate(),
                lastFallAt.atZone(zone).toLocalDate(),
            ).toInt().coerceAtLeast(0)
        } else null

        return FallsTabProjection(
            residentId = residentId,
            streakDays = streakDays,
            previousStreakDays = previousStreakDays,
            fallsLast12Months = fallsInRange.size,
            exitsLast12Months = exitsInRange.size,
            lastFallAt = lastFallAt,
            lastFallInjury = lastFall?.second,
            months = monthRange.map { ym ->
                FallMonthProjection(
                    label = ym.toString(),
                    falls = fallsInRange.count {
                        it.first.atZone(zone).toLocalDate().yearMonth == ym
                    },
                    exits = exitsInRange.count {
                        it.occurredAt.atZone(zone).toLocalDate().yearMonth == ym
                    },
                )
            },
        )
    }

    private val LocalDate.yearMonth: YearMonth get() = YearMonth.from(this)

    // ─────────────────────────────────────────────────────── episodes

    @Transactional(readOnly = true)
    fun getEpisodesTab(residentId: String): EpisodesTabProjection {
        val historyEpisodes = historyEpisodeRepository.findByResidentId(ResidentId(residentId))
        val engineEpisodes = episodeRepository.findByResidentId(ResidentId(residentId))
        val historyIds = historyEpisodes.map { it.id.value }.toSet()
        val reviews = historyEpisodes.map { ep ->
            historyReviewRepository.findByEpisodeId(ep.id)
        }
        val historyProjections = historyEpisodes.zip(reviews).map { (ep, revs) ->
            val lastReview = revs.maxByOrNull { it.resolvedAt ?: java.time.Instant.MIN }
            EpisodeListItemProjection(
                id = ep.id.value,
                kind = ep.kind.name,
                title = null,
                severity = ep.severity.name,
                occurredAt = ep.occurredAt,
                injuryStatus = ep.injuryStatus,
                selfRecovery = ep.selfRecovery,
                verdict = lastReview?.detectionVerdict,
                reviewNote = lastReview?.reviewNote,
                reviewedAt = lastReview?.resolvedAt,
            )
        }
        /* The engine and clinical history are separate stores. The chart must
         * not hide a live engine episode simply because history ingestion has
         * not produced its clinical projection yet. Once both stores contain
         * the same id, the history row wins because it has review data. */
        val engineProjections = engineEpisodes
            .filterNot { it.id.value in historyIds }
            .map { ep ->
                val lastReview = historyReviewRepository
                    .findByEpisodeId(HistoryEpisodeId(ep.id.value))
                    .maxByOrNull { it.resolvedAt ?: java.time.Instant.MIN }
                EpisodeListItemProjection(
                    id = ep.id.value,
                    kind = ep.trigger ?: ep.ruleId ?: "OTHER",
                    title = ep.title,
                    severity = ep.severity.name,
                    occurredAt = ep.occurredAt,
                    injuryStatus = null,
                    selfRecovery = null,
                    verdict = lastReview?.detectionVerdict,
                    reviewNote = lastReview?.reviewNote,
                    reviewedAt = lastReview?.resolvedAt,
                )
            }
        return EpisodesTabProjection(
            residentId = residentId,
            episodes = (historyProjections + engineProjections)
                .sortedByDescending { it.occurredAt },
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

    companion object {
        val DEFAULT_ZONE: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")
    }
}
