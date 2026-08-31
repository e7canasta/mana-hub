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
import com.hub.residence.domain.repository.BedRepository
import com.hub.residence.domain.repository.RoomRepository
import com.hub.residence.domain.repository.WingRepository
import com.hub.shared.domain.BedId
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
    private val bedRepository: BedRepository,
    private val roomRepository: RoomRepository,
    private val wingRepository: WingRepository,
) {

    /**
     * Resuelve cama -> habitacion -> ala.
     *
     * El panel muestra "Hab. 301 - Cama A" en cada fila del piso: sin esto la
     * lista es cuatro nombres sin lugar, y en una residencia el lugar es medio
     * dato. Antes devolvia RailLocation(null, null, null), o sea un objeto
     * vacio que el cliente no puede distinguir de "todavia no tiene cama".
     *
     * El cache es por llamada: el rail pide la ubicacion de cada residente y
     * varios comparten habitacion y ala, asi que sin memoizar son tres queries
     * por fila para leer los mismos dos registros.
     */
    private class LocationResolver(
        private val bedRepository: BedRepository,
        private val roomRepository: RoomRepository,
        private val wingRepository: WingRepository,
    ) {
        private val rooms = mutableMapOf<String, com.hub.residence.domain.model.Room?>()
        private val wings = mutableMapOf<String, com.hub.residence.domain.model.Wing?>()

        fun resolve(bedId: BedId): RailLocation? {
            val bed = bedRepository.findById(bedId) ?: return null
            val room = rooms.getOrPut(bed.roomId.value) { roomRepository.findById(bed.roomId) }
            val wing = room?.let { r ->
                wings.getOrPut(r.wingId.value) { wingRepository.findById(r.wingId) }
            }
            return RailLocation(
                wingName = wing?.name,
                roomNumber = room?.number,
                bedLabel = bed.label,
            )
        }
    }

    private fun locations() = LocationResolver(bedRepository, roomRepository, wingRepository)

    /**
     * Días antes de la admisión no son observables: el cubo/panel no deben
     * mostrar 14 ceros ficticios (Susan día 1–2). Cero = medido; pre-admisión = ausente.
     */
    private fun effectiveObservationFrom(residentId: String, from: LocalDate): LocalDate {
        val admission = residentRepository.findById(ResidentId(residentId))?.admissionDate
        return if (admission != null && admission.isAfter(from)) admission else from
    }

    private fun datesInRange(from: LocalDate, to: LocalDate): List<LocalDate> =
        generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) }
            .toList()

    // ──────────────────────────────────────────────────────── resident-rail

    @Transactional(readOnly = true)
    fun getResidentRail(): List<ResidentRailItem> {
        val residents = residentRepository.findAll()
        val resolver = locations()
        return residents.map { resident ->
            val assignment = bedAssignmentRepository.findOpenByResidentId(resident.id)
            val bedState = assignment?.let { bedStateRepository.findByBedId(it.bedId) }
            ResidentRailItem(
                id = resident.id.value,
                fullName = resident.fullName,
                location = assignment?.let { resolver.resolve(it.bedId) },
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
            location = assignment?.let { locations().resolve(it.bedId) },
            currentState = bedState?.let {
                RailState(state = it.state, staffPresent = null, stateSince = it.stateSince)
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
            )
        }
        val total = days.sumOf { it.totalMinutes }
        val proactive = days.sumOf { it.proactiveMinutes }
        return CareTabProjection(
            residentId = residentId,
            from = from.toString(),
            to = to.toString(),
            observedFrom = observedFrom.toString(),
            summaries = days,
            avgMinutesPerDay = if (days.isEmpty()) 0.0 else total.toDouble() / days.size,
            proactiveShare = if (total == 0) 0.0 else proactive.toDouble() / total,
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
        val firstObservedAt = episodes.minByOrNull { it.occurredAt }?.occurredAt
        val streakFrom = lastFallAt ?: firstObservedAt
        val streakDays = streakFrom?.let {
            java.time.temporal.ChronoUnit.DAYS.between(
                it.atZone(ZoneOffset.UTC).toLocalDate(), now
            ).toInt().coerceAtLeast(0)
        }

        val previousFall = falls.drop(1).firstOrNull()
        /* Null y no 0: "no hubo una caida anterior" y "la anterior fue el mismo
         * dia" son cosas distintas, y con 0 se leen igual. */
        val previousStreakDays = if (previousFall != null && lastFallAt != null) {
            java.time.temporal.ChronoUnit.DAYS.between(
                previousFall.occurredAt.atZone(ZoneOffset.UTC).toLocalDate(),
                lastFallAt.atZone(ZoneOffset.UTC).toLocalDate()
            ).toInt().coerceAtLeast(0)
        } else null

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
