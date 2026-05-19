package com.cqrs.event;

import java.time.Instant;
import java.util.UUID;

public record CustomerDataUpdatedMessage(
        UUID aggregateId,
        Instant occurredAt,
        String name,
        String address,
        Integer age
) {}
