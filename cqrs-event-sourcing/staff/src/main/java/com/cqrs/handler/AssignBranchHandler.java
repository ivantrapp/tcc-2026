package com.cqrs.handler;

import com.cqrs.domain.aggregate.AccountManagerAggregate;
import com.cqrs.domain.command.AssignBranchCommand;
import com.cqrs.domain.event.DomainEvent;
import com.cqrs.domain.event.EventMetadata;
import com.cqrs.domain.exception.DomainException;
import com.cqrs.eventstore.EventStore;
import com.cqrs.publisher.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignBranchHandler {

    private final EventStore eventStore;
    private final EventPublisher publisher;
    private final TransactionTemplate transactionTemplate;

    public void handle(AssignBranchCommand command) {
        // A transacao termina (commit e conexao devolvida ao pool) antes da publicacao,
        // para a conexao nao ficar presa enquanto o RabbitMQ responde.
        List<DomainEvent> events = transactionTemplate.execute(status -> {
            if (!eventStore.streamExists("Branch-" + command.branchId())) {
                throw new DomainException("Branch not found: " + command.branchId());
            }

            String streamId = "AccountManager-" + command.accountManagerId();

            AccountManagerAggregate manager = AccountManagerAggregate.empty();
            eventStore.loadStream(streamId).forEach(manager::applyStoredEvent);

            if (!manager.exists()) {
                throw new DomainException("AccountManager not found: " + command.accountManagerId());
            }

            int expectedVersion = manager.getVersion();
            EventMetadata metadata = new EventMetadata(command.correlationId(), command.causedBy());
            manager.assignBranch(command.branchId(), metadata);

            eventStore.append(streamId, expectedVersion, manager.getPendingEvents());

            return List.copyOf(manager.getPendingEvents());
        });

        events.forEach(publisher::publish);
    }
}
