package com.cqrs.handler;

import com.cqrs.domain.aggregate.CustomerAggregate;
import com.cqrs.domain.command.CreateCustomerCommand;
import com.cqrs.domain.event.DomainEvent;
import com.cqrs.domain.event.EventMetadata;
import com.cqrs.eventstore.EventStore;
import com.cqrs.client.StaffClient;
import com.cqrs.publisher.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateCustomerHandler {

    private final EventStore eventStore;
    private final EventPublisher publisher;
    private final StaffClient staffClient;
    private final TransactionTemplate transactionTemplate;

    public UUID handle(CreateCustomerCommand command) {
        staffClient.validateAccountManagerExists(command.accountManagerId());
        UUID customerId = UUID.randomUUID();

        // A transacao termina (commit e conexao devolvida ao pool) antes da publicacao,
        // para a conexao nao ficar presa enquanto o RabbitMQ responde.
        List<DomainEvent> events = transactionTemplate.execute(status -> {
            CustomerAggregate customer = CustomerAggregate.empty();
            int expectedVersion = customer.getVersion();

            EventMetadata metadata = new EventMetadata(command.correlationId(), command.causedBy());
            customer.create(customerId, command.name(), command.address(), command.age(), command.accountManagerId(), metadata);

            eventStore.append("Customer-" + customerId, expectedVersion, customer.getPendingEvents());

            return List.copyOf(customer.getPendingEvents());
        });

        events.forEach(publisher::publish);

        return customerId;
    }
}
