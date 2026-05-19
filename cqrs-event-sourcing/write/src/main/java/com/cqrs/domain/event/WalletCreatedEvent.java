package com.cqrs.domain.event;

import java.time.Instant;
import java.util.UUID;

public record WalletCreatedEvent(
        UUID eventId,
        String eventType,
        UUID aggregateId,
        String aggregateType,
        int version,
        Instant occurredAt,
        EventMetadata metadata,
        UUID customerId
) implements DomainEvent {

    public static WalletCreatedEvent create(UUID walletId, int version, EventMetadata metadata, UUID customerId) {
        return new WalletCreatedEvent(
                UUID.randomUUID(), "WalletCreated", walletId, "Wallet",
                version, Instant.now(), metadata, customerId
        );
    }
}
