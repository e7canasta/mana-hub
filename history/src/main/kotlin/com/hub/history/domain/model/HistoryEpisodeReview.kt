package com.hub.history.domain.model

import com.hub.shared.domain.AggregateRoot
import java.time.Instant

class HistoryEpisodeReview private constructor(
    override val id: HistoryEpisodeId,
    val episodeId: HistoryEpisodeId,
    val status: String,
    val detectionVerdict: String?,
    val reviewNote: String?,
    val resolvedAt: Instant?,
    val actorId: String,
    override var version: Long
) : AggregateRoot<HistoryEpisodeId>() {

    companion object {
        /**
         * El instante se **inyecta**, no se toma del reloj del sistema.
         *
         * Antes esta factoría llamaba a `Instant.now()`, y eso ponía la hora
         * real en una fila que convive con eventos en hora simulada: un episodio
         * del 3 de septiembre revisado el 31 de agosto, o sea revisado antes de
         * ocurrir. Y como el orden de "cuál es la última revisión" sale de este
         * campo, la consecuencia visible era que reclasificar no cambiaba nada.
         *
         * Es la misma disciplina que los motores de mana-hive declaran en su
         * encabezado — "Now is injected, never Instant.now()" — y que este lado
         * no tenía. Un objeto de dominio que lee el reloj del mundo no se puede
         * probar ni reproducir.
         */
        fun create(
            episodeId: HistoryEpisodeId,
            actorId: String,
            now: Instant,
            status: String = "pending",
            detectionVerdict: String? = null,
            reviewNote: String? = null,
        ): HistoryEpisodeReview = HistoryEpisodeReview(
            id = HistoryEpisodeId.random(), episodeId = episodeId, status = status,
            detectionVerdict = detectionVerdict, reviewNote = reviewNote,
            resolvedAt = if (status == "resolved") now else null,
            actorId = actorId, version = 0
        )

        fun reconstitute(
            id: HistoryEpisodeId, episodeId: HistoryEpisodeId, status: String, detectionVerdict: String?,
            reviewNote: String?, resolvedAt: Instant?, actorId: String, version: Long
        ): HistoryEpisodeReview = HistoryEpisodeReview(id, episodeId, status, detectionVerdict, reviewNote, resolvedAt, actorId, version)
    }
}
