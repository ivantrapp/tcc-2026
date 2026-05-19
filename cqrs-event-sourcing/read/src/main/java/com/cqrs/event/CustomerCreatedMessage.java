package com.cqrs.event;

import java.time.Instant;
import java.util.UUID;

public record CustomerCreatedMessage(
        UUID aggregateId,
        Instant occurredAt,
        String name,
        String address,
        int age,
        UUID accountManagerId
) {}
