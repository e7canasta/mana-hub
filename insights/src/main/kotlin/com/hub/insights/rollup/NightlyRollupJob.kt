package com.hub.insights.rollup

import com.hub.insights.config.InsightsProperties
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class NightlyRollupJob(
    private val rollupService: RollupService,
    private val properties: InsightsProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${insights.rollup.cron:0 0 6 * * *}", zone = "\${insights.timezone:America/Argentina/Buenos_Aires}")
    fun run() {
        if (!properties.rollup.enabled) {
            log.debug("nightly rollup disabled")
            return
        }
        val observedOn = LocalDate.now(properties.zoneId).minusDays(1)
        log.info("nightly rollup starting observedOn={}", observedOn)
        val results = rollupService.rollupAll(observedOn, publish = true)
        val done = results.count { !it.skipped }
        val skipped = results.count { it.skipped }
        log.info("nightly rollup done residents={} skipped={}", done, skipped)
    }
}
