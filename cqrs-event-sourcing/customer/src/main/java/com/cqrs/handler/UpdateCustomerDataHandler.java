package com.cqrs.handler;

import com.cqrs.domain.aggregate.CustomerAggregate;
import com.cqrs.domain.command.UpdateCustomerDataCommand;
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
public class UpdateCustomerDataHandler {

    private final EventStore eventStore;
    private final EventPublisher publisher;
    private final TransactionTemplate transactionTemplate;

    public void handle(UpdateCustomerDataCommand command) {
        // A transacao termina (commit e conexao devolvida ao pool) antes da publicacao,
        // para a conexao nao ficar presa enquanto o RabbitMQ responde.
        List<DomainEvent> events = transactionTemplate.execute(status -> {
            String streamId = "Customer-" + command.customerId();

            CustomerAggregate customer = CustomerAggregate.empty();
            eventStore.loadStream(streamId).forEach(customer::applyStoredEvent);

            if (!customer.exists()) {
                throw new DomainException("Customer not found: " + command.customerId());
            }

            int expectedVersion = customer.getVersion();
            EventMetadata metadata = new EventMetadata(command.correlationId(), command.causedBy());
            customer.updateData(command.name(), command.address(), command.age(), metadata);

            eventStore.append(streamId, expectedVersion, customer.getPendingEvents());

            return List.copyOf(customer.getPendingEvents());
        });

        events.forEach(publisher::publish);
    }
}
