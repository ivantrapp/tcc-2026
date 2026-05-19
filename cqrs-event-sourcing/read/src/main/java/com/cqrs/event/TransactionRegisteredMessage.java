package com.cqrs.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionRegisteredMessage(
        UUID aggregateId,
        Instant occurredAt,
        BigDecimal value,
        TransactionType type,
        UUID exchangeConfirmationId
) {}
