package com.cqrs.handler;

import com.cqrs.client.AccountManagerClient;
import com.cqrs.domain.aggregate.CustomerAggregate;
import com.cqrs.domain.command.AssignAccountManagerCommand;
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

@Service
@RequiredArgsConstructor
public class AssignAccountManagerHandler {

    private final EventStore eventStore;
    private final EventPublisher publisher;
    private final AccountManagerClient accountManagerClient;

    @Transactional
    public void handle(AssignAccountManagerCommand command) {
        String streamId = "Customer-" + command.customerId();

        if (!accountManagerClient.exists(command.accountManagerId())) {
            throw new DomainException("AccountManager not found: " + command.accountManagerId());
        }

        CustomerAggregate customer = CustomerAggregate.empty();
        eventStore.loadStream(streamId).forEach(customer::applyStoredEvent);

        if (!customer.exists()) {
            throw new DomainException("Customer not found: " + command.customerId());
        }

        int expectedVersion = customer.getVersion();
        EventMetadata metadata = new EventMetadata(command.correlationId(), command.causedBy());
        customer.assignAccountManager(command.accountManagerId(), metadata);

        eventStore.append(streamId, expectedVersion, customer.getPendingEvents());

        List<DomainEvent> events = List.copyOf(customer.getPendingEvents());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                events.forEach(publisher::publish);
            }
        });
    }
}
