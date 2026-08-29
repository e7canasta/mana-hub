package com.hub.bridge

import com.manahive.messaging.BusConnector
import com.manahive.messaging.BusEvents
import com.manahive.messaging.NatsClientConfiguration
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * Infra NATS resiliente — igual que mana-hive NightWatchApplication.kt:50 / HubInfrastructureConfiguration.kt:90
 * BusEvents + BusConnector con NatsConfig.connectAsync (no bloquea arranque, reconecta infinito).
 * Reutiliza cabeceras compartidas: NatsConfig, NatsTopology, NatsClientConfiguration, Subjects, EventEnvelope.
 */
@Configuration
@Import(NatsClientConfiguration::class)
class HubNatsConfig {

    @Bean
    @ConditionalOnProperty(name = ["nats.enabled"], havingValue = "true")
    fun busEvents(): BusEvents = BusEvents()

    @Bean
    @ConditionalOnProperty(name = ["nats.enabled"], havingValue = "true")
    fun busConnector(
        @Value("\${nats.url:nats://localhost:4222}") url: String,
        events: BusEvents
    ): BusConnector = BusConnector(url, events)
}
