package com.hub

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class ManaHubApplication

fun main(args: Array<String>) {
    runApplication<ManaHubApplication>(*args)
}
