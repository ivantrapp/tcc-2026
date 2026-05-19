package com.cqrs.domain.aggregate;

import com.cqrs.domain.event.*;
import com.cqrs.domain.exception.DomainException;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class AccountManagerAggregate {

    private UUID id;
    private String name;
    private UUID branchId;
    private boolean admin;
    private Instant createdAt;
    private int version = -1;

    private final List<DomainEvent> pendingEvents = new ArrayList<>();

    public static AccountManagerAggregate empty() {
        return new AccountManagerAggregate();
    }

    public boolean exists() {
        return id != null;
    }

    public void create(UUID accountManagerId, String name, UUID branchId,
                       boolean isAdmin, EventMetadata metadata) {
        AccountManagerCreatedEvent event = AccountManagerCreatedEvent.create(
                accountManagerId, version + 1, metadata, name, branchId, isAdmin
        );
        applyStoredEvent(event);
        pendingEvents.add(event);
    }

    public void grantAdminProfile(EventMetadata metadata) {
        if (admin) {
            throw new DomainException("AccountManager already has admin profile.");
        }
        AdminProfileGrantedEvent event = AdminProfileGrantedEvent.create(
                id, version + 1, metadata, metadata.causedBy()
        );
        applyStoredEvent(event);
        pendingEvents.add(event);
    }

    public void assignBranch(UUID newBranchId, EventMetadata metadata) {
        BranchAssignedEvent event = BranchAssignedEvent.create(
                id, version + 1, metadata, newBranchId
        );
        applyStoredEvent(event);
        pendingEvents.add(event);
    }

    public void applyStoredEvent(DomainEvent event) {
        if (event instanceof AccountManagerCreatedEvent e) {
            id = e.aggregateId();
            name = e.name();
            branchId = e.branchId();
            admin = e.isAdmin();
            createdAt = e.occurredAt();
        } else if (event instanceof AdminProfileGrantedEvent) {
            admin = true;
        } else if (event instanceof BranchAssignedEvent e) {
            branchId = e.branchId();
        }
        version = event.version();
    }
}
