package com.cqrs.domain.command;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateBranchCommand(
        @NotBlank String region,
        UUID correlationId,
        String causedBy
) {}
