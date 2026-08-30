package com.hub.bridge.ingest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.manahive.contracts.EventEnvelope
import io.nats.client.Connection
import io.nats.client.Dispatcher
import io.nats.client.JetStream
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy

/**
 * Suscriptor NATS que recibe eventos de mana-hive y los routea a mana-hub.
 *
 * Shared Kernel: deserializa EventEnvelope con los mismos tipos de contracts JAR.
 * EventRouter mapea cada tipo al endpoint correcto de hub.
 *
 * Patrón: Dispatcher callbacks (como NightWatchService), sin poll loops.
 */
@Service
class NatsIngestService(
    private val connection: Connection,
    private val objectMapper: ObjectMapper,
    private val eventRouter: EventRouter,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val dispatchers = mutableListOf<Dispatcher>()
    private lateinit var jetStream: JetStream
    private val busMapper = ObjectMapper().apply {
        registerModule(JavaTimeModule())
        registerModule(KotlinModule.Builder().build())
        disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    companion object {
        const val PERCEPTION = "perception.observation.v1.>"
        const val SCENE = "scene.fact.v1.>"
        const val SENTINEL = "sentinel.signal.v1.>"
        const val ALARM = "alarm.event.v1.>"
        const val RECORDER = "recorder.command.v1.>"
        const val EVIDENCE = "evidence.record.v1.>"
    }

    @PostConstruct
    fun init() {
        try {
            jetStream = connection.jetStream()

            subscribeTo(PERCEPTION, "perception")
            subscribeTo(SCENE, "scene")
            subscribeTo(SENTINEL, "sentinel")
            subscribeTo(ALARM, "alarm")
            subscribeTo(RECORDER, "recorder")
            subscribeTo(EVIDENCE, "evidence")

            log.info("Bridge suscrito a {} subjects NATS", dispatchers.size)
        } catch (e: Exception) {
            log.error("Bridge NATS subscribe failed: {}", e.message)
        }
    }

    private fun subscribeTo(subject: String, name: String) {
        val dispatcher = connection.createDispatcher { msg ->
            try {
                val raw = String(msg.data)
                log.info("NATS RECEIVED {} subject={} bytes={}", name, msg.subject, raw.length)
                val envelope = busMapper.readValue<EventEnvelope>(raw)
                log.info("NATS DESERIALIZED {} type={} eventId={}", name, envelope.type, envelope.eventId)
                eventRouter.route(envelope, msg.subject)
                msg.ack()
            } catch (e: Exception) {
                log.error("Failed to process {} from {}: {}", name, msg.subject, e.message)
            }
        }
        dispatcher.subscribe(subject)
        dispatchers += dispatcher
        log.debug("Subscribed to {} ({})", subject, name)
    }

    @PreDestroy
    fun close() {
        dispatchers.forEach { d ->
            try { connection.closeDispatcher(d) } catch (_: Exception) {}
        }
    }
}
