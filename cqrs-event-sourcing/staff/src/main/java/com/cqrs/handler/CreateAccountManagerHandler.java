package com.cqrs.handler;

import com.cqrs.domain.aggregate.AccountManagerAggregate;
import com.cqrs.domain.command.CreateAccountManagerCommand;
import com.cqrs.domain.event.DomainEvent;
import com.cqrs.domain.event.EventMetadata;
import com.cqrs.domain.exception.DomainException;
import com.cqrs.eventstore.EventStore;
import com.cqrs.publisher.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateAccountManagerHandler {

    private final EventStore eventStore;
    private final EventPublisher publisher;
    private final TransactionTemplate transactionTemplate;

    public UUID handle(CreateAccountManagerCommand command) {
        UUID accountManagerId = UUID.randomUUID();

        // A transacao termina (commit e conexao devolvida ao pool) antes da publicacao,
        // para a conexao nao ficar presa enquanto o RabbitMQ responde.
        List<DomainEvent> events = transactionTemplate.execute(status -> {
            if (!eventStore.streamExists("Branch-" + command.branchId())) {
                throw new DomainException("Branch not found: " + command.branchId());
            }

            AccountManagerAggregate manager = AccountManagerAggregate.empty();
            int expectedVersion = manager.getVersion();

            EventMetadata metadata = new EventMetadata(command.correlationId(), command.causedBy());
            manager.create(accountManagerId, command.name(), command.branchId(), command.isAdmin(), metadata);

            eventStore.append("AccountManager-" + accountManagerId, expectedVersion, manager.getPendingEvents());

            return List.copyOf(manager.getPendingEvents());
        });

        events.forEach(publisher::publish);

        return accountManagerId;
    }
}
