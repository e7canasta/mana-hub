package com.hub.history.domain.model

/**
 * Severidad de un episodio, en el vocabulario del dominio.
 *
 * Era LOW / MEDIUM / HIGH / CRITICAL, que mide *riesgo clinico*. El dominio del
 * panel mide otra cosa: **que obliga**. INFO avisa y nada mas; WARNING pide
 * mirar el video; CRITICAL manda a alguien a la habitacion; EMERGENCY lo manda
 * urgente. Esa matriz es la autoridad del director, no una etiqueta de color.
 *
 * Tener las dos escalas convivendo era peor que tener una sola equivocada,
 * porque la palabra CRITICAL existia en las dos con significados distintos: en
 * la vieja era el techo, en esta es el anteultimo escalon. Confundirlas no da
 * error, da una alarma de menos.
 *
 * `from()` acepta unicamente estos cuatro nombres, a proposito. Aceptar los
 * viejos "por compatibilidad" es lo que mantiene vivo el idioma que se quiso
 * retirar: si algo manda "medium", queremos enterarnos ahora y no que quede
 * traducido en silencio.
 */
enum class HistoryEpisodeSeverity {
    /** Queda registrado. No interrumpe a nadie. */
    INFO,

    /** Confirmacion en el panel, mirando el video. */
    WARNING,

    /** Hay que ir a la habitacion. Se graba. */
    CRITICAL,

    /** Hay que ir a la habitacion, urgente. Se graba. */
    EMERGENCY;

    companion object {
        fun from(value: String): HistoryEpisodeSeverity = when (value.lowercase()) {
            "info" -> INFO
            "warning" -> WARNING
            "critical" -> CRITICAL
            "emergency" -> EMERGENCY
            else -> throw IllegalArgumentException(
                "Severidad desconocida: '$value'. El dominio define INFO, WARNING, CRITICAL y EMERGENCY " +
                    "(vocabulario-unificado.md). Si esto viene de un cliente viejo con LOW/MEDIUM/HIGH, " +
                    "el arreglo es en el cliente: no se traduce aca."
            )
        }
    }
}
