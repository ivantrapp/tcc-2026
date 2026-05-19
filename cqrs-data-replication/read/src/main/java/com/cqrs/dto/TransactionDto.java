package com.cqrs.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionDto(
        UUID transactionId,
        UUID walletId,
        BigDecimal value,
        String type,
        UUID exchangeConfirmationId,
        Instant occurredAt
) {}
