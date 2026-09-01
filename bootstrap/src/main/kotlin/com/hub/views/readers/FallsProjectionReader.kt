package com.hub.views.readers

import com.hub.history.domain.repository.HistoryEpisodeDetectionRepository
import com.hub.population.domain.repository.BedAssignmentRepository
import com.hub.shared.domain.BedId
import com.hub.shared.domain.LocationResolver
import com.hub.shared.domain.ResidentId
import com.hub.views.FallMonthProjection
import com.hub.views.FallsTabProjection
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Component
class FallsProjectionReader(
    private val historyEpisodeRepository: HistoryEpisodeDetectionRepository,
    private val bedAssignmentRepository: BedAssignmentRepository,
    private val locationResolver: LocationResolver,
) {

    @Transactional(readOnly = true)
    fun getFallsTab(residentId: String, months: Int = 12): FallsTabProjection {
        val episodes = historyEpisodeRepository.findByResidentId(ResidentId(residentId))
        val falls = episodes.filter { it.kind.name == "FALL" }
            .sortedByDescending { it.occurredAt }

        val assignment = bedAssignmentRepository.findOpenByResidentId(ResidentId(residentId))
        val zone = assignment?.let { locationResolver.zone(it.bedId) } ?: DEFAULT_ZONE
        val now = LocalDate.now(zone)
        val monthRange = (0 until months).map { YearMonth.now(zone).minusMonths(it.toLong()) }

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
            ChronoUnit.DAYS.between(it.atZone(zone).toLocalDate(), now).toInt().coerceAtLeast(0)
        }

        val previousFall = falls.drop(1).firstOrNull()
        /* Null y no 0: "no hubo una caida anterior" y "la anterior fue el mismo
         * dia" son cosas distintas, y con 0 se leen igual. */
        val previousStreakDays = if (previousFall != null && lastFallAt != null) {
            ChronoUnit.DAYS.between(
                previousFall.occurredAt.atZone(zone).toLocalDate(),
                lastFallAt.atZone(zone).toLocalDate(),
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
                        it.occurredAt.atZone(zone).toLocalDate().yearMonth == ym
                    },
                )
            },
        )
    }

    private val LocalDate.yearMonth: YearMonth get() = YearMonth.from(this)

    companion object {
        private val DEFAULT_ZONE: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")
    }
}
