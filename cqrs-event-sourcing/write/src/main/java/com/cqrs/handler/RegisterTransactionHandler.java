package com.cqrs.handler;

import com.cqrs.domain.aggregate.WalletAggregate;
import com.cqrs.domain.aggregate.WalletStateSnapshot;
import com.cqrs.domain.command.RegisterTransactionCommand;
import com.cqrs.domain.event.DomainEvent;
import com.cqrs.domain.event.EventMetadata;
import com.cqrs.domain.exception.DomainException;
import com.cqrs.eventstore.EventStore;
import com.cqrs.eventstore.SnapshotRecord;
import com.cqrs.publisher.EventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegisterTransactionHandler {

    private static final int SNAPSHOT_THRESHOLD = 50;

    private final EventStore eventStore;
    private final EventPublisher publisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handle(RegisterTransactionCommand command) {
        String streamId = "Wallet-" + command.walletId();

        WalletAggregate wallet = reconstitute(command.walletId(), streamId);

        if (!wallet.exists()) {
            throw new DomainException("Wallet not found: " + command.walletId());
        }

        int expectedVersion = wallet.getVersion();

        EventMetadata metadata = new EventMetadata(command.correlationId(), command.causedBy());
        wallet.registerTransaction(command.value(), command.type(), command.exchangeConfirmationId(), metadata);

        eventStore.append(streamId, expectedVersion, wallet.getPendingEvents());

        if (wallet.getVersion() % SNAPSHOT_THRESHOLD == 0) {
            persistSnapshot(wallet);
        }

        List<DomainEvent> events = List.copyOf(wallet.getPendingEvents());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                events.forEach(publisher::publish);
            }
        });
    }

    private WalletAggregate reconstitute(java.util.UUID walletId, String streamId) {
        return eventStore.getLatestSnapshot(walletId)
                .map(snap -> {
                    try {
                        WalletStateSnapshot state = objectMapper.readValue(snap.getState(), WalletStateSnapshot.class);
                        WalletAggregate wallet = WalletAggregate.fromSnapshot(state);
                        List<DomainEvent> delta = eventStore.loadStream(streamId, snap.getVersion() + 1);
                        delta.forEach(wallet::applyStoredEvent);
                        return wallet;
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to load snapshot for wallet: " + walletId, e);
                    }
                })
                .orElseGet(() -> {
                    WalletAggregate wallet = WalletAggregate.empty();
                    eventStore.loadStream(streamId).forEach(wallet::applyStoredEvent);
                    return wallet;
                });
    }

    private void persistSnapshot(WalletAggregate wallet) {
        try {
            String state = objectMapper.writeValueAsString(wallet.toSnapshot());
            SnapshotRecord snapshot = SnapshotRecord.builder()
                    .aggregateId(wallet.getId())
                    .aggregateType("Wallet")
                    .version(wallet.getVersion())
                    .state(state)
                    .createdAt(Instant.now())
                    .build();
            eventStore.saveSnapshot(snapshot);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to persist snapshot for wallet: " + wallet.getId(), e);
        }
    }
}
