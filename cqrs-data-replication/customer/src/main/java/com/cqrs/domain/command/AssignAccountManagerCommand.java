package com.cqrs.domain.command;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignAccountManagerCommand(
        @NotNull UUID customerId,
        @NotNull UUID accountManagerId
) {}
