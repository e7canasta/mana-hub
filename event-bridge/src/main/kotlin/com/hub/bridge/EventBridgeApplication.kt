package com.hub.bridge

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EventBridgeApplication

fun main(args: Array<String>) {
    runApplication<EventBridgeApplication>(*args)
}
