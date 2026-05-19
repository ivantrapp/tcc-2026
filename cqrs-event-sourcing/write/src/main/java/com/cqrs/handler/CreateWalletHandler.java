package com.cqrs.handler;

import com.cqrs.domain.aggregate.WalletAggregate;
import com.cqrs.domain.command.CreateWalletCommand;
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
public class CreateWalletHandler {

    private final EventStore eventStore;
    private final EventPublisher publisher;

    @Transactional
    public UUID handle(CreateWalletCommand command) {
        String customerStream = "Customer-" + command.customerId();
        if (!eventStore.streamExists(customerStream)) {
            throw new DomainException("Customer not found: " + command.customerId());
        }

        UUID walletId = UUID.randomUUID();
        String streamId = "Wallet-" + walletId;

        WalletAggregate wallet = WalletAggregate.empty();
        int expectedVersion = wallet.getVersion();

        EventMetadata metadata = new EventMetadata(command.correlationId(), command.causedBy());
        wallet.create(walletId, command.customerId(), metadata);

        eventStore.append(streamId, expectedVersion, wallet.getPendingEvents());

        var events = List.copyOf(wallet.getPendingEvents());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                events.forEach(publisher::publish);
            }
        });

        return walletId;
    }
}
