package com.cqrs.domain.command;

import com.cqrs.domain.model.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record RegisterTransactionCommand(
        @NotNull UUID walletId,
        @NotNull @Positive BigDecimal value,
        @NotNull TransactionType type,
        @NotNull UUID exchangeConfirmationId,
        UUID correlationId,
        String causedBy
) {}
