package com.cqrs.handler;

import com.cqrs.domain.aggregate.AccountManagerAggregate;
import com.cqrs.domain.command.GrantAdminProfileCommand;
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
public class GrantAdminProfileHandler {

    private final EventStore eventStore;
    private final EventPublisher publisher;

    @Transactional
    public void handle(GrantAdminProfileCommand command) {
        String streamId = "AccountManager-" + command.accountManagerId();

        AccountManagerAggregate manager = AccountManagerAggregate.empty();
        eventStore.loadStream(streamId).forEach(manager::applyStoredEvent);

        if (!manager.exists()) {
            throw new DomainException("AccountManager not found: " + command.accountManagerId());
        }

        int expectedVersion = manager.getVersion();
        EventMetadata metadata = new EventMetadata(command.correlationId(), command.causedBy());
        manager.grantAdminProfile(metadata);

        eventStore.append(streamId, expectedVersion, manager.getPendingEvents());

        List<DomainEvent> events = List.copyOf(manager.getPendingEvents());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                events.forEach(publisher::publish);
            }
        });
    }
}
