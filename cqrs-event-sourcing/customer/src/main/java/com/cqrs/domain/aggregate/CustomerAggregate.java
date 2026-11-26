package com.cqrs.domain.aggregate;

import com.cqrs.domain.event.*;
import com.cqrs.domain.exception.DomainException;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class CustomerAggregate {

    private UUID id;
    private String name;
    private String address;
    private int age;
    private UUID accountManagerId;
    private Instant createdAt;
    private int version = -1;

    private final List<DomainEvent> pendingEvents = new ArrayList<>();

    public static CustomerAggregate empty() {
        return new CustomerAggregate();
    }

    public boolean exists() {
        return id != null;
    }

    public void create(UUID customerId, String name, String address, int age,
                       UUID accountManagerId, EventMetadata metadata) {
        validateAge(age);

        CustomerCreatedEvent event = CustomerCreatedEvent.create(
                customerId, version + 1, metadata, name, address, age, accountManagerId
        );
        applyStoredEvent(event);
        pendingEvents.add(event);
    }

    public void updateData(String name, String address, Integer age, EventMetadata metadata) {
        if (age != null) validateAge(age);

        CustomerDataUpdatedEvent event = CustomerDataUpdatedEvent.create(
                id, version + 1, metadata, name, address, age
        );
        applyStoredEvent(event);
        pendingEvents.add(event);
    }

    public void assignAccountManager(UUID newAccountManagerId, EventMetadata metadata) {
        AccountManagerAssignedEvent event = AccountManagerAssignedEvent.create(
                id, version + 1, metadata, accountManagerId, newAccountManagerId
        );
        applyStoredEvent(event);
        pendingEvents.add(event);
    }

    public void applyStoredEvent(DomainEvent event) {
        if (event instanceof CustomerCreatedEvent e) {
            id = e.aggregateId();
            name = e.name();
            address = e.address();
            age = e.age();
            accountManagerId = e.accountManagerId();
            createdAt = e.occurredAt();
        } else if (event instanceof CustomerDataUpdatedEvent e) {
            if (e.name() != null) name = e.name();
            if (e.address() != null) address = e.address();
            if (e.age() != null) age = e.age();
        } else if (event instanceof AccountManagerAssignedEvent e) {
            accountManagerId = e.newAccountManagerId();
        }
        version = event.version();
    }

    private void validateAge(int age) {
        if (age < 18) {
            throw new DomainException("Customer must be at least 18 years old.");
        }
    }
}
