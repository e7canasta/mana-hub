package com.hub.bridge.ingest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hub.bridge.domain.RawEvent
import io.nats.client.Connection
import io.nats.client.JetStreamSubscription
import io.nats.client.PushSubscribeOptions
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import java.time.Instant

/**
 * Suscriptor NATS que recibe eventos de mana-hibe y los hace POST a mana-hub.
 *
 * El bridge es un Translation Layer puro:
 * - Escucha NATS
 * - POST a mana-hub
 * - No almacena nada
 */
@Service
class NatsIngestService(
    private val connection: Connection,
    private val objectMapper: ObjectMapper,
    @Value("\${bridge.nats.durable:bridge}") private val durable: String,
    @Value("\${bridge.target.url:http://localhost:8080}") private val targetUrl: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client = RestClient.builder().baseUrl(targetUrl).build()
    private val subscriptions = mutableListOf<JetStreamSubscription>()

    companion object {
        const val PERCEPTION = "perception.observation.v1.>"
        const val SCENE = "scene.fact.v1.>"
        const val SENTINEL = "sentinel.signal.v1.>"
        const val ALARM = "alarm.event.v1.>"
    }

    @PostConstruct
    fun init() {
        try {
            val js = connection.jetStream()
            subscriptions += js.subscribe(PERCEPTION, PushSubscribeOptions.builder().durable("$durable-perception").deliverGroup("bridge").build()).also { pollLoop(it, "perception") }
            subscriptions += js.subscribe(SCENE, PushSubscribeOptions.builder().durable("$durable-scene").deliverGroup("bridge").build()).also { pollLoop(it, "scene") }
            subscriptions += js.subscribe(SENTINEL, PushSubscribeOptions.builder().durable("$durable-sentinel").deliverGroup("bridge").build()).also { pollLoop(it, "sentinel") }
            subscriptions += js.subscribe(ALARM, PushSubscribeOptions.builder().durable("$durable-alarm").deliverGroup("bridge").build()).also { pollLoop(it, "alarm") }
            log.info("Bridge suscrito a {} subjects NATS", subscriptions.size)
        } catch (e: Exception) {
            log.error("Bridge NATS subscribe failed: {}", e.message)
        }
    }

    private fun pollLoop(sub: JetStreamSubscription, subject: String) {
        Thread {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val msg = sub.nextMessage(java.time.Duration.ofSeconds(1)) ?: continue
                    forwardToHub(msg.data, subject)
                    msg.ack()
                } catch (_: InterruptedException) { break }
                catch (_: Exception) { }
            }
        }.apply { isDaemon = true; name = "bridge-$subject"; start() }
    }

    @PreDestroy
    fun close() {
        subscriptions.forEach { try { it.unsubscribe() } catch (_: Exception) {} }
    }

    private fun forwardToHub(data: ByteArray, subject: String) {
        try {
            val raw = objectMapper.readValue<Map<String, Any>>(data)
            val event = mapOf(
                "eventId" to raw["eventId"],
                "type" to raw["type"],
                "version" to raw["version"],
                "occurredAt" to raw["occurredAt"],
                "source" to raw["source"],
                "subject" to subject,
                "payloadJson" to raw["payloadJson"],
            )

            client.post()
                .uri("/webhooks/events")
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(event))
                .retrieve()
                .body(String::class.java)

            log.debug("Forwarded {} from {} to hub", raw["type"], subject)
        } catch (e: Exception) {
            log.error("Failed to forward event to hub: {}", e.message)
        }
    }
}
