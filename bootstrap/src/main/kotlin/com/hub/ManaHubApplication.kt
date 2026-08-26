package com.hub

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ManaHubApplication

fun main(args: Array<String>) {
    runApplication<ManaHubApplication>(*args)
}
