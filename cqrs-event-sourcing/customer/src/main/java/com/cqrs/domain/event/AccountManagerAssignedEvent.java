package com.cqrs.domain.event;

import java.time.Instant;
import java.util.UUID;

public record AccountManagerAssignedEvent(
        UUID eventId,
        String eventType,
        UUID aggregateId,
        String aggregateType,
        int version,
        Instant occurredAt,
        EventMetadata metadata,
        UUID previousAccountManagerId,
        UUID newAccountManagerId
) implements DomainEvent {

    public static AccountManagerAssignedEvent create(UUID customerId, int version, EventMetadata metadata,
                                                     UUID previousAccountManagerId, UUID newAccountManagerId) {
        return new AccountManagerAssignedEvent(
                UUID.randomUUID(), "AccountManagerAssigned", customerId, "Customer",
                version, Instant.now(), metadata, previousAccountManagerId, newAccountManagerId
        );
    }
}
