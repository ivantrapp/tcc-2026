package com.cqrs.domain.event;

import java.time.Instant;
import java.util.UUID;

public record AdminProfileGrantedEvent(
        UUID eventId,
        String eventType,
        UUID aggregateId,
        String aggregateType,
        int version,
        Instant occurredAt,
        EventMetadata metadata,
        String grantedBy
) implements DomainEvent {

    public static AdminProfileGrantedEvent create(UUID accountManagerId, int version,
                                                  EventMetadata metadata, String grantedBy) {
        return new AdminProfileGrantedEvent(
                UUID.randomUUID(), "AdminProfileGranted", accountManagerId, "AccountManager",
                version, Instant.now(), metadata, grantedBy
        );
    }
}
