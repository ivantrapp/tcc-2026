package com.cqrs.handler;

import com.cqrs.domain.aggregate.BranchAggregate;
import com.cqrs.domain.command.CreateBranchCommand;
import com.cqrs.domain.event.DomainEvent;
import com.cqrs.domain.event.EventMetadata;
import com.cqrs.eventstore.EventStore;
import com.cqrs.publisher.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateBranchHandler {

    private final EventStore eventStore;
    private final EventPublisher publisher;
    private final TransactionTemplate transactionTemplate;

    public UUID handle(CreateBranchCommand command) {
        UUID branchId = UUID.randomUUID();

        // A transacao termina (commit e conexao devolvida ao pool) antes da publicacao,
        // para a conexao nao ficar presa enquanto o RabbitMQ responde.
        List<DomainEvent> events = transactionTemplate.execute(status -> {
            BranchAggregate branch = BranchAggregate.empty();
            int expectedVersion = branch.getVersion();

            EventMetadata metadata = new EventMetadata(command.correlationId(), command.causedBy());
            branch.create(branchId, command.region(), metadata);

            eventStore.append("Branch-" + branchId, expectedVersion, branch.getPendingEvents());

            return List.copyOf(branch.getPendingEvents());
        });

        events.forEach(publisher::publish);

        return branchId;
    }
}
