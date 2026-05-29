package com.cqrs.domain.event;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    UUID eventId();
    String eventType();
    UUID aggregateId();
    String aggregateType();
    int version();
    Instant occurredAt();
    EventMetadata metadata();
}
