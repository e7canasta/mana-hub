package com.hub.clients.surveillance

import com.hub.clients.care.EpisodeNoteKind
import com.hub.clients.core.HttpApi
import com.hub.clients.core.SurveillanceDsl
import java.time.Instant

@SurveillanceDsl
class SurveillanceScope internal constructor(private val http: HttpApi) {

    // ══════════════════════════════════════════════════════════════
    //  EPISODE — Event requiring attention
    //  Vocabulary: docs/vocabulario-unificado.md §3.4
    // ══════════════════════════════════════════════════════════════

    /**
     * Registers an episode that requires attention.
     *
     * An episode groups multiple related scene changes
     * and follows the chain: opening → events → resolution.
     *
     * @param residentId affected resident
     * @param block builder with severity, title, detail, etc.
     */
    fun registerEpisode(residentId: String, block: EpisodeBuilder.() -> Unit): Episode {
        val builder = EpisodeBuilder(residentId).apply(block)
        val resp = http.post(
            "/api/v1/episodes",
            builder.toRequest(),
            EpisodeResponse::class.java
        )
        return Episode(http, resp)
    }

    @Deprecated("Use registerEpisode()", ReplaceWith("registerEpisode(residentId, block)"))
    fun triggerEpisode(residentId: String, block: EpisodeBuilder.() -> Unit): Episode =
        registerEpisode(residentId, block)

    fun pendingEpisodes(): List<EpisodeResponse> =
        http.get("/api/v1/episodes", Array<EpisodeResponse>::class.java).toList()

    fun episodes(): List<EpisodeResponse> =
        http.get("/api/v1/episodes", Array<EpisodeResponse>::class.java).toList()

    fun episodesByResident(residentId: String): List<EpisodeResponse> =
        episodes().filter { it.residentId == residentId }

    fun episodesBySeverity(severity: EpisodeSeverity): List<EpisodeResponse> =
        episodes().filter { it.severity == severity }

    fun episodesPending(): List<EpisodeResponse> =
        episodes().filter { it.isPending }

    fun episodeById(id: String): Episode? {
        return try {
            val resp = http.get("/api/v1/episodes/$id", EpisodeResponse::class.java)
            Episode(http, resp)
        } catch (_: Exception) {
            null
        }
    }
}

@SurveillanceDsl
class EpisodeBuilder(private val residentId: String) {
    var bedId: String? = null
    var severity: EpisodeSeverity = EpisodeSeverity.WARNING
    var title: String? = null
    var detail: String? = null
    var occurredAt: Instant = Instant.now()
    var evidenceKind: String? = null
    var evidenceRef: String? = null

    internal fun toRequest() = CreateEpisodeRequest(
        residentId = residentId,
        bedId = bedId,
        severity = severity,
        title = title,
        detail = detail,
        occurredAt = occurredAt,
        evidenceKind = evidenceKind,
        evidenceRef = evidenceRef
    )
}

class Episode internal constructor(
    private val http: HttpApi,
    raw: EpisodeResponse
) {
    var raw: EpisodeResponse = raw
        private set

    val id: String get() = raw.id
    val residentId: String get() = raw.residentId
    val severity: EpisodeSeverity get() = raw.severity
    val status: String get() = raw.status
    val title: String? get() = raw.title
    val isPending: Boolean get() = raw.isPending

    fun acknowledge(actorId: String): Episode {
        raw = http.post("/api/v1/episodes/$id/acknowledge", AcknowledgeEpisodeRequest(actorId), EpisodeResponse::class.java)
        return this
    }

    fun resolve(status: EpisodeStatus): Episode {
        raw = http.patch("/api/v1/episodes/$id", UpdateEpisodeRequest(status = status.name.lowercase()), EpisodeResponse::class.java)
        return this
    }

    @Deprecated("Use resolve(EpisodeStatus)", ReplaceWith("resolve(EpisodeStatus.RESOLVED)"))
    fun resolve(status: String = "resolved"): Episode {
        raw = http.patch("/api/v1/episodes/$id", UpdateEpisodeRequest(status = status), EpisodeResponse::class.java)
        return this
    }

    fun addNote(authorId: String, kind: EpisodeNoteKind, body: String): Episode {
        http.post("/api/v1/episodes/$id/notes", CreateEpisodeNoteRequest(id, authorId, kind, body), Any::class.java)
        return this
    }

    fun refresh(): Episode {
        raw = http.get("/api/v1/episodes/$id", EpisodeResponse::class.java)
        return this
    }

    override fun toString(): String = "Episode($severity, $status)"
}
