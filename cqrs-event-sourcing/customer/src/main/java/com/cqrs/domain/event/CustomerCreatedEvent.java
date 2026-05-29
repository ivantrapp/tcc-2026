package com.cqrs.domain.event;

import java.time.Instant;
import java.util.UUID;

public record CustomerCreatedEvent(
        UUID eventId,
        String eventType,
        UUID aggregateId,
        String aggregateType,
        int version,
        Instant occurredAt,
        EventMetadata metadata,
        String name,
        String address,
        int age,
        UUID accountManagerId
) implements DomainEvent {

    public static CustomerCreatedEvent create(UUID customerId, int version, EventMetadata metadata,
                                              String name, String address, int age, UUID accountManagerId) {
        return new CustomerCreatedEvent(
                UUID.randomUUID(), "CustomerCreated", customerId, "Customer",
                version, Instant.now(), metadata, name, address, age, accountManagerId
        );
    }
}
