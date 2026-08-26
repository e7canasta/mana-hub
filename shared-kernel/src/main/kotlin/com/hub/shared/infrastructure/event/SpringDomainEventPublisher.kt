package com.hub.shared.infrastructure.event

import com.hub.shared.domain.DomainEvent
import com.hub.shared.domain.DomainEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
public class SpringDomainEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) : DomainEventPublisher {

    private val log = LoggerFactory.getLogger(javaClass)

    public override fun publish(event: DomainEvent) {
        log.debug("Publishing domain event: ${event.eventType} [${event.eventId}]")
        applicationEventPublisher.publishEvent(event)
    }

    public override fun publishAll(events: List<DomainEvent>) {
        events.forEach { publish(it) }
    }
}
