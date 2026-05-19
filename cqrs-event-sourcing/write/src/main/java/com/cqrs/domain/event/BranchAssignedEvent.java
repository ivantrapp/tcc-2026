package com.cqrs.domain.event;

import java.time.Instant;
import java.util.UUID;

public record BranchAssignedEvent(
        UUID eventId,
        String eventType,
        UUID aggregateId,
        String aggregateType,
        int version,
        Instant occurredAt,
        EventMetadata metadata,
        UUID branchId
) implements DomainEvent {

    public static BranchAssignedEvent create(UUID accountManagerId, int version,
                                             EventMetadata metadata, UUID branchId) {
        return new BranchAssignedEvent(
                UUID.randomUUID(), "BranchAssigned", accountManagerId, "AccountManager",
                version, Instant.now(), metadata, branchId
        );
    }
}
