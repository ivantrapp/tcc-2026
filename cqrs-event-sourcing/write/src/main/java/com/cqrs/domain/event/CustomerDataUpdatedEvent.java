package com.cqrs.domain.event;

import java.time.Instant;
import java.util.UUID;

public record CustomerDataUpdatedEvent(
        UUID eventId,
        String eventType,
        UUID aggregateId,
        String aggregateType,
        int version,
        Instant occurredAt,
        EventMetadata metadata,
        String name,
        String address,
        Integer age
) implements DomainEvent {

    public static CustomerDataUpdatedEvent create(UUID customerId, int version, EventMetadata metadata,
                                                  String name, String address, Integer age) {
        return new CustomerDataUpdatedEvent(
                UUID.randomUUID(), "CustomerDataUpdated", customerId, "Customer",
                version, Instant.now(), metadata, name, address, age
        );
    }
}
