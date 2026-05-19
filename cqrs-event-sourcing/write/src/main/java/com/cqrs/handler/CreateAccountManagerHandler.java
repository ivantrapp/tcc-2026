package com.cqrs.handler;

import com.cqrs.domain.aggregate.AccountManagerAggregate;
import com.cqrs.domain.command.CreateAccountManagerCommand;
import com.cqrs.domain.event.DomainEvent;
import com.cqrs.domain.event.EventMetadata;
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
public class CreateAccountManagerHandler {

    private final EventStore eventStore;
    private final EventPublisher publisher;

    @Transactional
    public UUID handle(CreateAccountManagerCommand command) {
        UUID accountManagerId = UUID.randomUUID();

        AccountManagerAggregate manager = AccountManagerAggregate.empty();
        int expectedVersion = manager.getVersion();

        EventMetadata metadata = new EventMetadata(command.correlationId(), command.causedBy());
        manager.create(accountManagerId, command.name(), command.branchId(), command.isAdmin(), metadata);

        eventStore.append("AccountManager-" + accountManagerId, expectedVersion, manager.getPendingEvents());

        List<DomainEvent> events = List.copyOf(manager.getPendingEvents());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                events.forEach(publisher::publish);
            }
        });

        return accountManagerId;
    }
}
