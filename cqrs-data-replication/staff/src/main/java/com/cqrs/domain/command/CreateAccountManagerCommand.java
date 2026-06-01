package com.cqrs.domain.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateAccountManagerCommand(
        @NotBlank String name,
        @NotNull UUID branchId,
        boolean isAdmin
) {}
