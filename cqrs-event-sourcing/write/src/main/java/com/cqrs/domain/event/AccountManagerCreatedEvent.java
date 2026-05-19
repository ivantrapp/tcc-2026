package com.cqrs.domain.event;

import java.time.Instant;
import java.util.UUID;

public record AccountManagerCreatedEvent(
        UUID eventId,
        String eventType,
        UUID aggregateId,
        String aggregateType,
        int version,
        Instant occurredAt,
        EventMetadata metadata,
        String name,
        UUID branchId,
        boolean isAdmin
) implements DomainEvent {

    public static AccountManagerCreatedEvent create(UUID accountManagerId, int version, EventMetadata metadata,
                                                    String name, UUID branchId, boolean isAdmin) {
        return new AccountManagerCreatedEvent(
                UUID.randomUUID(), "AccountManagerCreated", accountManagerId, "AccountManager",
                version, Instant.now(), metadata, name, branchId, isAdmin
        );
    }
}
