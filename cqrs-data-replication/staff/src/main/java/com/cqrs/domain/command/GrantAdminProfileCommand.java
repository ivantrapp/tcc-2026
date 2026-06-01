package com.cqrs.domain.command;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GrantAdminProfileCommand(
        @NotNull UUID accountManagerId
) {}
