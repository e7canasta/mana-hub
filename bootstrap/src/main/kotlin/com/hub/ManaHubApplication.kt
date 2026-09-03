package com.hub

import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class ManaHubApplication {
    @org.springframework.context.annotation.Bean
    fun buildInfo(): InfoContributor = InfoContributor { builder ->
        builder.withDetail("build", mapOf(
            "version" to BUILD_VERSION,
            "component" to "mana-hub",
        ))
    }
}

const val BUILD_VERSION = "envelope-v2-2026-09-02"

fun main(args: Array<String>) {
    runApplication<ManaHubApplication>(*args)
}
