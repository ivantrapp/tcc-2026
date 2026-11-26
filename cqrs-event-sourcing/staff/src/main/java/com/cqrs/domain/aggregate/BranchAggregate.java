package com.cqrs.domain.aggregate;

import com.cqrs.domain.event.BranchCreatedEvent;
import com.cqrs.domain.event.DomainEvent;
import com.cqrs.domain.event.EventMetadata;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class BranchAggregate {

    private UUID id;
    private String region;
    private Instant createdAt;
    private int version = -1;

    private final List<DomainEvent> pendingEvents = new ArrayList<>();

    public static BranchAggregate empty() {
        return new BranchAggregate();
    }

    public void create(UUID branchId, String region, EventMetadata metadata) {
        BranchCreatedEvent event = BranchCreatedEvent.create(branchId, version + 1, metadata, region);
        applyStoredEvent(event);
        pendingEvents.add(event);
    }

    public void applyStoredEvent(DomainEvent event) {
        if (event instanceof BranchCreatedEvent e) {
            id = e.aggregateId();
            region = e.region();
            createdAt = e.occurredAt();
        }
        version = event.version();
    }
}
