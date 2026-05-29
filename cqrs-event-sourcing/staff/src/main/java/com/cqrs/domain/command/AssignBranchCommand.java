package com.cqrs.domain.command;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignBranchCommand(
        @NotNull UUID accountManagerId,
        @NotNull UUID branchId,
        UUID correlationId,
        String causedBy
) {}
