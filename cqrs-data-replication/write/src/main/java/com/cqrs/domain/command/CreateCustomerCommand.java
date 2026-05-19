package com.cqrs.domain.command;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCustomerCommand(
        @NotBlank String name,
        @NotBlank String address,
        @Min(18) int age,
        @NotNull UUID accountManagerId
) {}
