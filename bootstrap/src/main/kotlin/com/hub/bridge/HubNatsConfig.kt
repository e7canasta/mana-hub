/**
 * SOURCE OF TRUTH KEYWORDS: hub_nats_connection, NATS, outbound_events, nats.enabled
 * WHAT: Creates the optional NATS connection used by mana-hub outbound event publishers.
 * WHY: Hub publishes confirmed SOR facts but does not consume NATS commands yet.
 * WHERE: Injected by NatsDomainEventPublisher in the bootstrap application.
 */
package com.hub.bridge

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["nats.enabled"], havingValue = "true")
class HubNatsConfig
