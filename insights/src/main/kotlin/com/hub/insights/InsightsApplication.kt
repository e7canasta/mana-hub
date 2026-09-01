package com.hub.insights

import com.hub.insights.config.InsightsProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@ConfigurationPropertiesScan
@EnableScheduling
@EnableConfigurationProperties(InsightsProperties::class)
class InsightsConfiguration
