package com.cqrs.handler;

import com.cqrs.domain.aggregate.WalletAggregate;
import com.cqrs.domain.command.CreateWalletCommand;
import com.cqrs.domain.event.DomainEvent;
import com.cqrs.domain.event.EventMetadata;
import com.cqrs.eventstore.EventStore;
import com.cqrs.client.CustomerClient;
import com.cqrs.publisher.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateWalletHandler {

    private final EventStore eventStore;
    private final EventPublisher publisher;
    private final CustomerClient customerClient;
    private final TransactionTemplate transactionTemplate;

    public UUID handle(CreateWalletCommand command) {
        customerClient.validateCustomerExists(command.customerId());
        UUID walletId = UUID.randomUUID();

        // A transacao termina (commit e conexao devolvida ao pool) antes da publicacao,
        // para a conexao nao ficar presa enquanto o RabbitMQ responde.
        List<DomainEvent> events = transactionTemplate.execute(status -> {
            String streamId = "Wallet-" + walletId;

            WalletAggregate wallet = WalletAggregate.empty();
            int expectedVersion = wallet.getVersion();

            EventMetadata metadata = new EventMetadata(command.correlationId(), command.causedBy());
            wallet.create(walletId, command.customerId(), metadata);

            eventStore.append(streamId, expectedVersion, wallet.getPendingEvents());

            return List.copyOf(wallet.getPendingEvents());
        });

        events.forEach(publisher::publish);

        return walletId;
    }
}
