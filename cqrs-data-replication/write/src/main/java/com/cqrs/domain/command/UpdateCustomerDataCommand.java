package com.cqrs.domain.command;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateCustomerDataCommand(
        @NotNull UUID customerId,
        String name,
        String address,
        @Min(18) Integer age
) {}
