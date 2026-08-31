package com.hub.insights.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.LocalTime
import java.time.ZoneId

@ConfigurationProperties(prefix = "insights")
data class InsightsProperties(
    val hubUrl: String = "http://localhost:8080",
    val timezone: String = "America/Argentina/Buenos_Aires",
    val sleepWindow: SleepWindowProperties = SleepWindowProperties(),
    val baselineMinDays: Int = 7,
    val walkingMetersPerMinute: Double = 50.0,
    /** Minutos continuos en Lying para contar sueño profundo (calm). */
    val deepSleepAfterMinutes: Int = 10,
    val rollup: RollupProperties = RollupProperties(),
) {
    val zoneId: ZoneId get() = ZoneId.of(timezone)
}

data class SleepWindowProperties(
    val start: String = "19:00",
    val end: String = "12:00",
) {
    val startTime: LocalTime get() = LocalTime.parse(start)
    val endTime: LocalTime get() = LocalTime.parse(end)
}

data class RollupProperties(
    val enabled: Boolean = false,
    val cron: String = "0 0 6 * * *",
)
