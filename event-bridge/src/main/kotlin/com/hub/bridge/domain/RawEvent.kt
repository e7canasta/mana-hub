package com.hub.bridge.domain

import java.time.Instant

/**
 * Evento crudo que llega de mana-hub vía NATS.
 * El bridge lo traduce a formato REST.
 */
data class RawEvent(
    val eventId: String,
    val type: String,
    val version: Int,
    val occurredAt: Instant,
    val source: String,
    val subject: String,
    val payloadJson: String,
)
