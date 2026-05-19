package com.cqrs.domain.command;

import jakarta.validation.constraints.NotBlank;

public record CreateBranchCommand(
        @NotBlank String region
) {}
