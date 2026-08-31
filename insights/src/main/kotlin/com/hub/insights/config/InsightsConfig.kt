package com.hub.insights.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class InsightsConfig(
    private val properties: InsightsProperties,
) {
    @Bean
    fun hubRestClient(): RestClient =
        RestClient.builder()
            .baseUrl(properties.hubUrl.trimEnd('/'))
            .build()
}
