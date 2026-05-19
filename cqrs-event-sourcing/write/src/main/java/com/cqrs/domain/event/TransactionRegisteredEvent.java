package com.cqrs.domain.event;

import com.cqrs.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionRegisteredEvent(
        UUID eventId,
        String eventType,
        UUID aggregateId,
        String aggregateType,
        int version,
        Instant occurredAt,
        EventMetadata metadata,
        BigDecimal value,
        TransactionType type,
        UUID exchangeConfirmationId
) implements DomainEvent {

    public static TransactionRegisteredEvent create(UUID walletId, int version, EventMetadata metadata,
                                                    BigDecimal value, TransactionType type, UUID exchangeConfirmationId) {
        return new TransactionRegisteredEvent(
                UUID.randomUUID(), "TransactionRegistered", walletId, "Wallet",
                version, Instant.now(), metadata, value, type, exchangeConfirmationId
        );
    }
}
