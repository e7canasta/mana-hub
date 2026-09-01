package com.hub.surveillance.support

import com.hub.shared.domain.BedId
import com.hub.shared.domain.EpisodeId
import com.hub.shared.domain.ResidentId
import com.hub.surveillance.domain.model.Episode
import com.hub.surveillance.domain.model.EpisodeSeverity
import com.hub.surveillance.domain.model.EpisodeStatus
import java.time.Instant

/**
 * Lenguaje Ubicuo — surveillance: el libro de guardia.
 *
 * Director: "Se abrió un episodio CRITICAL para María a las 3:12 por borde de cama.
 * La enfermera lo reconoció a las 3:15 y se resolvió a las 3:20."
 *
 * Vernon: Episode es Aggregate Root con ciclo PENDING → ACKNOWLEDGED → RESOLVED + escalamiento.
 * Fowler: DSL como "Episode Mother" + Given/When/Then en español.
 */

inline fun dado(ctx: String, block: () -> Unit) = block()
inline fun cuando(accion: String, block: () -> Unit) = block()
inline fun entonces(esperado: String, block: () -> Unit) = block()

@DslMarker annotation class EpisodioDsl

@EpisodioDsl
class EpisodioBuilder {
    var residente: String = "maria-1"
    var cama: String? = "cama-12"
    var gravedad: EpisodeSeverity = EpisodeSeverity.WARNING
    var titulo: String? = "Borde de cama detectado"
    var detalle: String? = "María al borde durante 5 min"
    var cuando: Instant = Instant.parse("2026-09-01T03:12:00Z")
    var id: EpisodeId? = null

    fun build(): Episode = Episode.create(
        residentId = ResidentId(residente),
        bedId = cama?.let { BedId(it) },
        severity = gravedad,
        title = titulo,
        detail = detalle,
        occurredAt = cuando,
        id = id
    )
}

fun episodio(block: EpisodioBuilder.() -> Unit) = EpisodioBuilder().apply(block).build()

// Atajos legibles
fun episodioCritico(residente: String = "maria-1") = episodio { this.residente = residente; gravedad = EpisodeSeverity.CRITICAL }
fun episodioInformativo(residente: String = "juan-1") = episodio { this.residente = residente; gravedad = EpisodeSeverity.INFO; titulo = "Solo registro" }

// Fowler: Object Mother para casos repetidos en specs
object EpisodiosMadre {
    fun pendiente(maria: String = "maria-1") = episodio { residente = maria; gravedad = EpisodeSeverity.WARNING }
    fun reconocido(maria: String = "maria-1", por: String = "enfermera.ana") = pendiente(maria).acknowledge(por)
    fun resuelto(maria: String = "maria-1", por: String = "enfermera.ana") = reconocido(maria, por).resolve(por)
}

fun severityDesde(texto: String) = EpisodeSeverity.from(texto)
fun statusDesde(texto: String) = EpisodeStatus.from(texto)
