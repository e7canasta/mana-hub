package com.hub.bridge

import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class EventBridgeApplication {
    @Bean
    fun buildInfo(): InfoContributor = InfoContributor { builder ->
        builder.withDetail("build", mapOf(
            "version" to BUILD_VERSION,
            "component" to "mana-bridge",
        ))
    }
}

const val BUILD_VERSION = "envelope-v2-2026-09-02"

fun main(args: Array<String>) {
    runApplication<EventBridgeApplication>(*args)
}
