package com.emergency.events;

import com.emergency.domain.ApplicationEvent;
import com.emergency.repository.ApplicationEventRepository;
import com.emergency.service.LogicalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publishes structured application events to the database.
 * Used by the logical service layer to emit business-meaningful events.
 */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final ApplicationEventRepository eventRepository;

    public EventPublisher(ApplicationEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void publish(EventType type, LogicalService service, String requestId,
                        String message, String metadata) {
        try {
            String eventId = "evt-" + UUID.randomUUID().toString().substring(0, 8);
            ApplicationEvent event = new ApplicationEvent(
                    eventId, requestId, type.name(), service.name(), message, metadata
            );
            eventRepository.save(event);
            log.debug("Event published: {} requestId={} service={}", type, requestId, service);
        } catch (Exception ex) {
            log.error("Failed to publish event {}: {}", type, ex.getMessage());
        }
    }
}
