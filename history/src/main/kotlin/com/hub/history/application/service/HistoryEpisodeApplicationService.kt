package com.hub.history.application.service

import com.hub.history.application.dto.*
import com.hub.history.domain.model.*
import com.hub.history.domain.repository.HistoryEpisodeDetectionRepository
import com.hub.history.domain.repository.HistoryEpisodeReviewRepository
import com.hub.shared.domain.ResidentId
import com.hub.shared.domain.BedId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Service
class HistoryEpisodeApplicationService(
    private val detectionRepository: HistoryEpisodeDetectionRepository,
    private val reviewRepository: HistoryEpisodeReviewRepository
) {

    @Transactional
    fun ingestHistoryEpisode(request: IngestHistoryEpisodeRequest): HistoryEpisodeResponse {
        val detection = HistoryEpisode.create(
            sourceRecordId = request.sourceRecordId,
            residentId = ResidentId(request.residentId),
            bedId = request.bedId?.let { BedId(it) },
            kind = request.kind,
            severity = request.severity,
            occurredAt = request.occurredAt,
            source = request.source
        )
        return toResponse(detectionRepository.save(detection))
    }

    @Transactional(readOnly = true)
    fun getResidentHistoryEpisodes(residentId: String): List<HistoryEpisodeResponse> {
        return detectionRepository.findByResidentId(ResidentId(residentId)).map { toResponse(it) }
    }

    /**
     * Un episodio suelto por su id.
     *
     * Faltaba, y sin esto el panel no puede abrir un episodio desde el listado:
     * la fila se clickea, la columna del acto pide el episodio por id y no hay
     * a quien preguntarle. Traerlo filtrando la lista del residente tampoco
     * sirve, porque desde el id no se sabe de que residente es.
     */
    @Transactional(readOnly = true)
    fun getHistoryEpisode(episodeId: String): HistoryEpisodeResponse? {
        return detectionRepository.findById(HistoryEpisodeId(episodeId))?.let { toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getHistoryEpisodeSequence(episodeId: String): List<HistoryEpisodeReviewResponse> {
        return reviewRepository.findByEpisodeId(HistoryEpisodeId(episodeId)).map { toReviewResponse(it) }
    }

    @Transactional
    fun reviewHistoryEpisode(episodeId: String, request: ReviewHistoryEpisodeRequest): HistoryEpisodeReviewResponse {
        val review = HistoryEpisodeReview.create(
            episodeId = HistoryEpisodeId(episodeId),
            actorId = request.actorId,
            status = request.status,
            detectionVerdict = request.detectionVerdict,
            reviewNote = request.reviewNote,
        )
        return toReviewResponse(reviewRepository.save(review))
    }

    @Transactional(readOnly = true)
    fun getFallsSummary(residentId: String, months: Int = 12): FallsSummaryResponse {
        val rid = ResidentId(residentId)
        val falls = detectionRepository.findByResidentIdAndKind(rid, EpisodeKind.FALL)
            .sortedByDescending { it.occurredAt }

        val now = LocalDate.now()
        val cutoff = now.minusMonths(months.toLong())

        val fallsInRange = falls.filter {
            val date = it.occurredAt.atZone(ZoneOffset.UTC).toLocalDate()
            date.isAfter(cutoff)
        }

        val streakDays = calculateStreak(fallsInRange, now)
        val previousStreak = calculatePreviousStreak(fallsInRange)

        val lastFall = falls.firstOrNull()

        val monthLabels = (0 until months).map { now.minusMonths(it.toLong()) }
            .map { YearMonth.from(it) }
            .reversed()

        val fallsByMonth = fallsInRange.groupBy {
            YearMonth.from(it.occurredAt.atZone(ZoneOffset.UTC).toLocalDate())
        }

        val monthSummaries = monthLabels.map { ym ->
            FallsMonthSummary(
                label = ym.toString(),
                falls = fallsByMonth[ym]?.size ?: 0
            )
        }

        return FallsSummaryResponse(
            residentId = residentId,
            streakDays = streakDays,
            previousStreakDays = previousStreak,
            fallsLast12Months = fallsInRange.size,
            lastFallAt = lastFall?.occurredAt?.toString(),
            lastFallInjury = lastFall?.injuryStatus,
            months = monthSummaries
        )
    }

    private fun calculateStreak(falls: List<HistoryEpisode>, today: LocalDate): Int {
        if (falls.isEmpty()) return 0
        val lastFallDate = falls.first().occurredAt.atZone(ZoneOffset.UTC).toLocalDate()
        return ChronoUnit.DAYS.between(lastFallDate, today).toInt()
    }

    private fun calculatePreviousStreak(falls: List<HistoryEpisode>): Int {
        if (falls.size < 2) return 0
        val secondLastFall = falls[1].occurredAt.atZone(ZoneOffset.UTC).toLocalDate()
        val firstFall = falls[0].occurredAt.atZone(ZoneOffset.UTC).toLocalDate()
        return ChronoUnit.DAYS.between(secondLastFall, firstFall).toInt()
    }

    private fun toResponse(e: HistoryEpisode) = HistoryEpisodeResponse(
        id = e.id.value, sourceRecordId = e.sourceRecordId, residentId = e.residentId.value,
        bedId = e.bedId?.value, kind = e.kind, severity = e.severity, occurredAt = e.occurredAt,
        narrative = e.narrative, source = e.source
    )

    private fun toReviewResponse(r: HistoryEpisodeReview) = HistoryEpisodeReviewResponse(
        id = r.id.value, episodeId = r.episodeId.value, status = r.status,
        detectionVerdict = r.detectionVerdict, reviewNote = r.reviewNote,
        resolvedAt = r.resolvedAt, actorId = r.actorId
    )
}
