package com.hub.insights

import com.hub.insights.config.InsightsProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableConfigurationProperties(InsightsProperties::class)
class InsightsApplication

fun main(args: Array<String>) {
    runApplication<InsightsApplication>(*args)
}
