package com.cqrs.handler;

import com.cqrs.client.AccountManagerClient;
import com.cqrs.domain.aggregate.CustomerAggregate;
import com.cqrs.domain.command.CreateCustomerCommand;
import com.cqrs.domain.event.DomainEvent;
import com.cqrs.domain.event.EventMetadata;
import com.cqrs.domain.exception.DomainException;
import com.cqrs.eventstore.EventStore;
import com.cqrs.publisher.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateCustomerHandler {

    private final EventStore eventStore;
    private final EventPublisher publisher;
    private final AccountManagerClient accountManagerClient;

    @Transactional
    public UUID handle(CreateCustomerCommand command) {
        if (!accountManagerClient.exists(command.accountManagerId())) {
            throw new DomainException("AccountManager not found: " + command.accountManagerId());
        }

        UUID customerId = UUID.randomUUID();
        CustomerAggregate customer = CustomerAggregate.empty();
        int expectedVersion = customer.getVersion();

        EventMetadata metadata = new EventMetadata(command.correlationId(), command.causedBy());
        customer.create(customerId, command.name(), command.address(), command.age(), command.accountManagerId(), metadata);

        eventStore.append("Customer-" + customerId, expectedVersion, customer.getPendingEvents());

        List<DomainEvent> events = List.copyOf(customer.getPendingEvents());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                events.forEach(publisher::publish);
            }
        });

        return customerId;
    }
}
