package com.cqrs.domain.event;

import java.time.Instant;
import java.util.UUID;

public record BranchCreatedEvent(
        UUID eventId,
        String eventType,
        UUID aggregateId,
        String aggregateType,
        int version,
        Instant occurredAt,
        EventMetadata metadata,
        String region
) implements DomainEvent {

    public static BranchCreatedEvent create(UUID branchId, int version, EventMetadata metadata, String region) {
        return new BranchCreatedEvent(
                UUID.randomUUID(), "BranchCreated", branchId, "Branch",
                version, Instant.now(), metadata, region
        );
    }
}
