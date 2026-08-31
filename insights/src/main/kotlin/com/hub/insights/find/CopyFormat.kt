package com.hub.insights.find

import java.time.LocalTime
import java.util.Locale
import kotlin.math.roundToInt

object CopyFormat {

    fun clock(minutes: Int): String {
        val abs = kotlin.math.abs(minutes)
        val h = abs / 60
        val m = abs % 60
        return when {
            h == 0 -> "$m min"
            m == 0 -> "${h}h"
            else -> "${h}h ${m.toString().padStart(2, '0')}"
        }
    }

    fun oneDecimal(value: Double): String = "%.1f".format(Locale.US, value)

    fun percent(share: Double): String = "${(share * 100).roundToInt()}%"

    fun firstName(fullName: String?): String? =
        fullName?.trim()?.split(Regex("\\s+"))?.firstOrNull()?.takeIf { it.isNotBlank() }

    fun veces(n: Int): String = when (n) {
        1 -> "una vez"
        2 -> "dos veces"
        3 -> "tres veces"
        4 -> "cuatro veces"
        5 -> "cinco veces"
        else -> "$n veces"
    }

    fun clockTime(time: LocalTime): String = "${time.hour}:${time.minute.toString().padStart(2, '0')}"
}
