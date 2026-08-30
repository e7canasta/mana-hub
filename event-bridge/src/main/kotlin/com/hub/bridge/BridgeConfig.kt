package com.hub.bridge

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.nats.client.Connection
import io.nats.client.Nats
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class BridgeConfig {

    @Bean(destroyMethod = "close")
    fun natsConnection(
        @Value("\${nats.url:nats://localhost:4222}") url: String,
    ): Connection {
        return Nats.connect(url)
    }

    @Bean
    fun bridgeObjectMapper(): ObjectMapper {
        return jacksonObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }

    @Bean
    fun bridgeRestClient(
        @Value("\${bridge.target.url:http://localhost:8080}") targetUrl: String,
    ): RestClient {
        return RestClient.builder().baseUrl(targetUrl).build()
    }
}
